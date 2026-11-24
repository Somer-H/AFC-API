package com.afcgym.platform.service.impl;

import com.afcgym.platform.persistance.entities.CoachEntity;
import com.afcgym.platform.persistance.repositories.ICoachRepository;
import com.afcgym.platform.service.ICoachService;
import com.afcgym.platform.service.dto.CreateCoachCommand;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoachServiceImpl implements ICoachService {

    private final ICoachRepository coachRepository;

    @Override
    @Transactional
    public CoachEntity create(CreateCoachCommand command) {
        return coachRepository.save(CoachEntity.builder()
                .name(command.name().trim())
                .avatarUrl(command.avatarUrl())
                .createdAt(Instant.now())
                .build());
    }

    @Override
    public List<CoachEntity> list() {
        return coachRepository.findAll(Sort.by("name"));
    }
}
