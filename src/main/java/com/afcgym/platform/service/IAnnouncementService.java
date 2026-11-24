package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.AnnouncementEntity;
import com.afcgym.platform.service.dto.PublishAnnouncementCommand;
import java.util.List;

public interface IAnnouncementService {

    AnnouncementEntity publish(PublishAnnouncementCommand command);

    List<AnnouncementEntity> list();

    List<AnnouncementEntity> listCurrent(String sessionId);

    void dismiss(Long id, String sessionId);

    void delete(Long id);
}
