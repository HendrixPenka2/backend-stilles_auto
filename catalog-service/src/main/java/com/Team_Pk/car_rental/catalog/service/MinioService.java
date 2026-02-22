package com.Team_Pk.car_rental.catalog.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${minio.url}")
    private String minioUrl;

    /**
     * Uploade un fichier vers MinIO et retourne son URL publique.
     */
    public Mono<String> uploadFile(FilePart filePart) {
        // Génère un nom de fichier unique (ex: 550e8400-e29b-41d4-a716-446655440000.jpg)
        String extension = getExtension(filePart.filename());
        String filename = UUID.randomUUID().toString() + extension;

        // En WebFlux, un fichier est un flux de données (DataBuffer). On le transforme en InputStream.
        return DataBufferUtils.join(filePart.content())
                .flatMap(dataBuffer -> Mono.fromCallable(() -> {
                    try (InputStream inputStream = dataBuffer.asInputStream(true)) {
                        
                        minioClient.putObject(
                                PutObjectArgs.builder()
                                        .bucket(bucketName)
                                        .object(filename)
                                        .stream(inputStream, dataBuffer.readableByteCount(), -1)
                                        .contentType(filePart.headers().getContentType().toString())
                                        .build()
                        );
                        
                        // Retourne l'URL directe vers l'image
                        return minioUrl + "/" + bucketName + "/" + filename;
                        
                    } catch (Exception e) {
                        log.error("Erreur lors de l'upload vers MinIO", e);
                        throw new RuntimeException("Échec de l'upload de l'image");
                    } finally {
                        DataBufferUtils.release(dataBuffer); // Libère la mémoire
                    }
                }).subscribeOn(Schedulers.boundedElastic())); // Exécute hors du thread principal
    }

    /**
     * Supprime un fichier de MinIO à partir de son URL
     */
    public Mono<Void> deleteFile(String fileUrl) {
        return Mono.fromRunnable(() -> {
            try {
                // Extrait le nom du fichier depuis l'URL
                String filename = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(filename)
                                .build()
                );
            } catch (Exception e) {
                log.error("Erreur lors de la suppression sur MinIO", e);
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        return (dotIndex == -1) ? "" : filename.substring(dotIndex);
    }
}