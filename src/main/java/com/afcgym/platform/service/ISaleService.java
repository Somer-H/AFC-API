package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.SaleEntity;
import com.afcgym.platform.service.dto.ExportedFile;
import com.afcgym.platform.service.dto.RegisterSaleCommand;
import com.afcgym.platform.service.dto.SaleLineCommand;
import java.time.LocalDate;
import java.util.List;

public interface ISaleService {

    void validateStock(List<SaleLineCommand> lines);

    SaleEntity register(RegisterSaleCommand command);

    List<SaleEntity> listByDateRange(LocalDate from, LocalDate to);

    ExportedFile export(LocalDate from, LocalDate to);

    SaleEntity getById(Long id);
}
