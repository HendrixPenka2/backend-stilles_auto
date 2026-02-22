package com.Team_Pk.car_rental.catalog.service;


import com.Team_Pk.car_rental.catalog.dto.ImageReorderRequest;
import com.Team_Pk.car_rental.catalog.entity.VehicleImage;
import com.Team_Pk.car_rental.catalog.repository.VehicleImageRepository;
import com.Team_Pk.car_rental.catalog.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleImageService {

    private final VehicleImageRepository imageRepository;
    private final VehicleRepository vehicleRepository;
    private final MinioService minioService;
    

    public Flux<VehicleImage> uploadImages(UUID vehicleId, Flux<FilePart> files) {
        return vehicleRepository.findById(vehicleId)
                .switchIfEmpty(Mono.error(new RuntimeException("Véhicule introuvable")))
                .flatMapMany(vehicle -> 
                    imageRepository.countByVehicleId(vehicleId)
                        .flatMapMany(count -> 
                            files
                                .index() // Magie WebFlux : numérote chaque fichier (0, 1, 2...)
                                .concatMap(tuple -> { // concatMap traite les fichiers 1 par 1 (séquentiel)
                                    long index = tuple.getT1(); // Le numéro du fichier (0, 1, etc.)
                                    FilePart filePart = tuple.getT2(); // Le fichier en lui-même
                                    
                                    // isPrimary est TRUE si c'est la toute première image du véhicule
                                    boolean isPrimary = (count == 0 && index == 0); 
                                    
                                    return minioService.uploadFile(filePart)
                                        .flatMap(url -> {
                                            VehicleImage image = VehicleImage.builder()
                                                    .vehicleId(vehicleId)
                                                    .url(url)
                                                    .isPrimary(isPrimary)
                                                    .displayOrder((int) (count + index))
                                                    .uploadedAt(Instant.now())
                                                    .build();
                                            return imageRepository.save(image);
                                        });
                                })
                        )
                );
    }
    public Mono<Void> deleteImage(UUID vehicleId, UUID imageId) {
        return imageRepository.findById(imageId)
                .filter(img -> img.getVehicleId().equals(vehicleId))
                .switchIfEmpty(Mono.error(new RuntimeException("Image introuvable ou n'appartient pas à ce véhicule")))
                .flatMap(img -> 
                    // Supprime de MinIO puis de la BDD
                    minioService.deleteFile(img.getUrl())
                            .then(imageRepository.delete(img))
                );
    }

    // ==========================================
    // RÉORGANISER LES IMAGES
    // ==========================================
    public Mono<Void> reorderImages(UUID vehicleId, ImageReorderRequest request) {
        // 1. On "éteint" le is_primary sur toutes les images pour éviter le conflit PostgreSQL
        return imageRepository.resetPrimaryStatus(vehicleId)
                .thenMany(
                        // 2. On boucle sur la liste envoyée par le frontend
                        Flux.fromIterable(request.getOrder())
                                .flatMap(imgOrder -> {
                                    // Celle qui a l'ordre 0 devient la nouvelle image principale !
                                    boolean isPrimary = (imgOrder.getDisplayOrder() == 0);
                                    
                                    return imageRepository.updateImageOrderAndPrimary(
                                            imgOrder.getImageId(),
                                            vehicleId,
                                            imgOrder.getDisplayOrder(),
                                            isPrimary
                                    );
                                })
                )
                .then(); // On renvoie un Mono<Void> quand tout est fini
    }
}
