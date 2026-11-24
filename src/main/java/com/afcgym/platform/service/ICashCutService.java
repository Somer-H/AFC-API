package com.afcgym.platform.service;

import com.afcgym.platform.service.dto.CashCutCategory;
import com.afcgym.platform.service.dto.CashCutExport;
import com.afcgym.platform.service.dto.CashCutPeriod;
import com.afcgym.platform.service.dto.CashCutReport;
import java.time.LocalDate;

public interface ICashCutService {

    CashCutReport generate(CashCutPeriod period, CashCutCategory category, LocalDate reference);

    CashCutExport export(CashCutPeriod period, CashCutCategory category, LocalDate reference);
}
