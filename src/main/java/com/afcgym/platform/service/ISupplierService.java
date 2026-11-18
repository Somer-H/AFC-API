package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.SupplierEntity;
import com.afcgym.platform.service.dto.CreateSupplierCommand;
import java.util.List;

public interface ISupplierService {

    SupplierEntity create(CreateSupplierCommand command);

    List<SupplierEntity> list();
}
