package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.service.dto.RenewMembershipCommand;
import java.util.List;

public interface IPaymentService {

    PaymentEntity renew(Long memberId, RenewMembershipCommand command);

    List<PaymentEntity> listByMember(Long memberId);

    PaymentEntity approve(Long paymentId);
}
