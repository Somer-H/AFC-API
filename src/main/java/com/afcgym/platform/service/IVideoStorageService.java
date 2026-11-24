package com.afcgym.platform.service;

import com.afcgym.platform.service.dto.StoredVideo;
import com.afcgym.platform.service.dto.VideoUpload;

public interface IVideoStorageService {

    String store(VideoUpload upload);

    StoredVideo load(String fileName);

    void deleteAfterCommit(String fileName);
}
