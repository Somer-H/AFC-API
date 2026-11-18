package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.CoachEntity;
import com.afcgym.platform.service.dto.CreateCoachCommand;
import java.util.List;

public interface ICoachService {

    CoachEntity create(CreateCoachCommand command);

    List<CoachEntity> list();
}
