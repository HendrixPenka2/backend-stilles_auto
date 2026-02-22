package com.Team_Pk.car_rental.catalog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;

import com.Team_Pk.car_rental.catalog.dto.ImageReorderRequest;
import com.Team_Pk.car_rental.catalog.entity.AccessoryImage;
import com.Team_Pk.car_rental.catalog.repository.AccessoryImageRepository;
import com.Team_Pk.car_rental.catalog.repository.AccessoryRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessoryImageService {

    private final AccessoryImageRepository imageRepository;
    private final AccessoryRepository accessoryRepository;
    private final MinioService minioService;

    public Flux<AccessoryImage> uploadImages(UUID accessoryId, Flux<FilePart> files) {
        return accessoryRepository.findById(accessoryId)
            .switchIfEmpty(Mono.error(new RuntimeException("Accessoire introuvable")))
            .flatMapMany(acc -> imageRepository.countByAccessoryId(accessoryId)
                .flatMapMany(count -> files.index().concatMap(tuple -> {
                    long index = tuple.getT1();
                    FilePart file = tuple.getT2();
                    boolean isPrimary = (count == 0 && index == 0);
                    return minioService.uploadFile(file).flatMap(url -> 
                        imageRepository.save(AccessoryImage.builder()
                            .accessoryId(accessoryId).url(url).isPrimary(isPrimary)
                            .displayOrder((int)(count + index)).uploadedAt(Instant.now()).build())
                    );
                })));
    }

    public Mono<Void> deleteImage(UUID accessoryId, UUID imageId) {
        return imageRepository.findById(imageId)
            .filter(img -> img.getAccessoryId().equals(accessoryId))
            .switchIfEmpty(Mono.error(new RuntimeException("Image introuvable")))
            .flatMap(img -> minioService.deleteFile(img.getUrl()).then(imageRepository.delete(img)));
    }

    public Mono<Void> reorderImages(UUID accessoryId, ImageReorderRequest request) {
        return imageRepository.resetPrimaryStatus(accessoryId)
                .thenMany(Flux.fromIterable(request.getOrder())
                        .flatMap(order -> imageRepository.updateImageOrderAndPrimary(
                                order.getImageId(), accessoryId, order.getDisplayOrder(), order.getDisplayOrder() == 0)))
                .then();
    }
}
