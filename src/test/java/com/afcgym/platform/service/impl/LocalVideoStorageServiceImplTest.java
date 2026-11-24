package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.afcgym.platform.configuration.StorageProperties;
import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.service.dto.StoredVideo;
import com.afcgym.platform.service.dto.VideoUpload;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalVideoStorageServiceImplTest {

    private static final byte[] VIDEO_BYTES = {0, 0, 0, 24, 102, 116, 121, 112};

    @TempDir
    private Path videosDirectory;

    private LocalVideoStorageServiceImpl storage;

    @BeforeEach
    void init() {
        storage = new LocalVideoStorageServiceImpl(new StorageProperties(videosDirectory.toString()));
    }

    @Test
    void givenAnMp4_whenStore_thenSaveItUnderARandomNameThatCanBeLoadedBack() throws IOException {
        String fileName = storage.store(upload("video/mp4", VIDEO_BYTES));

        assertTrue(fileName.matches("[0-9a-f]{32}\\.mp4"));
        StoredVideo video = storage.load(fileName);
        assertEquals("video/mp4", video.contentType());
        try (InputStream content = video.resource().getInputStream()) {
            assertArrayEquals(VIDEO_BYTES, content.readAllBytes());
        }
    }

    @Test
    void givenANonVideoFile_whenStore_thenThrowBadRequestException() {
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> storage.store(upload("application/pdf", VIDEO_BYTES)));

        assertEquals("Solo se aceptan videos MP4, MOV o WEBM", exception.getMessage());
    }

    @Test
    void givenAnEmptyFile_whenStore_thenThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> storage.store(upload("video/mp4", new byte[0])));
    }

    @Test
    void givenAPathTraversalName_whenLoad_thenThrowNotFoundException() {
        assertThrows(NotFoundException.class, () -> storage.load("../application.yml"));
    }

    @Test
    void givenAWellFormedButMissingName_whenLoad_thenThrowNotFoundException() {
        assertThrows(NotFoundException.class, () -> storage.load("0123456789abcdef0123456789abcdef.mp4"));
    }

    @Test
    void givenAStoredVideoAndNoTransaction_whenDeleteAfterCommit_thenDeleteItRightAway() {
        String fileName = storage.store(upload("video/webm", VIDEO_BYTES));

        storage.deleteAfterCommit(fileName);

        assertFalse(Files.exists(videosDirectory.resolve(fileName)));
    }

    private VideoUpload upload(String contentType, byte[] bytes) {
        return new VideoUpload(new ByteArrayInputStream(bytes), contentType, bytes.length);
    }
}
