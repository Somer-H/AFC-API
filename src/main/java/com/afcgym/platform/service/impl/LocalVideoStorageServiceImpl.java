package com.afcgym.platform.service.impl;

import com.afcgym.platform.configuration.StorageProperties;
import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.service.IVideoStorageService;
import com.afcgym.platform.service.dto.StoredVideo;
import com.afcgym.platform.service.dto.VideoUpload;
import com.afcgym.platform.utils.SecureTokenGenerator;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalVideoStorageServiceImpl implements IVideoStorageService {

    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "video/mp4", "mp4",
            "video/quicktime", "mov",
            "video/webm", "webm");
    private static final Map<String, String> CONTENT_TYPE_BY_EXTENSION = Map.of(
            "mp4", "video/mp4",
            "mov", "video/quicktime",
            "webm", "video/webm");
    private static final Pattern STORED_FILE_NAME = Pattern.compile("^[0-9a-f]{32}\\.(mp4|mov|webm)$");
    private static final String VIDEO_NOT_FOUND = "No se encontró el video solicitado";

    private final StorageProperties properties;

    @Override
    public String store(VideoUpload upload) {
        if (upload.size() <= 0) {
            throw new BadRequestException("El archivo de video está vacío");
        }
        String extension = upload.contentType() == null
                ? null
                : EXTENSION_BY_CONTENT_TYPE.get(upload.contentType().toLowerCase(Locale.ROOT));
        if (extension == null) {
            throw new BadRequestException("Solo se aceptan videos MP4, MOV o WEBM");
        }
        String fileName = SecureTokenGenerator.generate(16) + "." + extension;
        Path target = directory().resolve(fileName);
        try {
            Files.createDirectories(directory());
            Files.copy(upload.content(), target);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        deleteQuietly(target);
                    }
                }
            });
        }
        return fileName;
    }

    @Override
    public StoredVideo load(String fileName) {
        if (fileName == null || !STORED_FILE_NAME.matcher(fileName).matches()) {
            throw new NotFoundException(VIDEO_NOT_FOUND);
        }
        Path path = directory().resolve(fileName);
        if (!Files.isRegularFile(path)) {
            throw new NotFoundException(VIDEO_NOT_FOUND);
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        return new StoredVideo(new FileSystemResource(path), CONTENT_TYPE_BY_EXTENSION.get(extension));
    }

    @Override
    public void deleteAfterCommit(String fileName) {
        if (fileName == null || !STORED_FILE_NAME.matcher(fileName).matches()) {
            return;
        }
        Path path = directory().resolve(fileName);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteQuietly(path);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteQuietly(path);
            }
        });
    }

    private Path directory() {
        return Path.of(properties.videosDirectory()).toAbsolutePath().normalize();
    }

    private void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("Could not delete video file {}", path, exception);
        }
    }
}
