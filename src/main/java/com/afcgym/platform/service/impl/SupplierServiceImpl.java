package com.afcgym.platform.service.impl;

import com.afcgym.platform.persistance.entities.SupplierEntity;
import com.afcgym.platform.persistance.repositories.ISupplierRepository;
import com.afcgym.platform.service.ISupplierService;
import com.afcgym.platform.service.dto.CreateSupplierCommand;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements ISupplierService {

    private final ISupplierRepository supplierRepository;

    @Override
    @Transactional
    public SupplierEntity create(CreateSupplierCommand command) {
        SupplierEntity supplier = SupplierEntity.builder()
                .name(command.name())
                .contact(command.contact())
                .phone(command.phone())
                .createdAt(Instant.now())
                .build();
        return supplierRepository.save(supplier);
    }

    @Override
    public List<SupplierEntity> list() {
        return supplierRepository.findAll();
    }
}
