package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.afcgym.platform.persistance.entities.SupplierEntity;
import com.afcgym.platform.persistance.repositories.ISupplierRepository;
import com.afcgym.platform.service.dto.CreateSupplierCommand;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTest {

    @Mock
    private ISupplierRepository supplierRepository;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    @Test
    void givenACommand_whenCreate_thenSaveAndReturnTheSupplier() {
        CreateSupplierCommand command = new CreateSupplierCommand("Proveedor Uno", "contacto@proveedor.com", "5512345678");
        SupplierEntity saved = SupplierEntity.builder()
                .id(1L).name("Proveedor Uno").contact("contacto@proveedor.com").phone("5512345678")
                .createdAt(Instant.now()).build();
        when(supplierRepository.save(any(SupplierEntity.class))).thenReturn(saved);

        SupplierEntity result = supplierService.create(command);

        assertEquals(saved, result);
    }

    @Test
    void whenList_thenReturnAllSuppliers() {
        SupplierEntity supplier = SupplierEntity.builder().id(1L).name("Proveedor Uno").createdAt(Instant.now()).build();
        when(supplierRepository.findAll()).thenReturn(List.of(supplier));

        List<SupplierEntity> result = supplierService.list();

        assertEquals(1, result.size());
    }
}
