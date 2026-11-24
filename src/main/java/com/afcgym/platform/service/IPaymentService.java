package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.service.dto.GatewayNotification;
import com.afcgym.platform.service.dto.MemberPaymentView;
import com.afcgym.platform.service.dto.RenewMembershipCommand;
import com.afcgym.platform.service.dto.Requester;
import com.afcgym.platform.service.dto.StartMemberPaymentCommand;
import java.time.LocalDateTime;
import java.util.List;

public interface IPaymentService {

    PaymentEntity renew(Long memberId, RenewMembershipCommand command);

    List<PaymentEntity> listByMember(Long memberId);

    PaymentEntity approve(Long paymentId, Long adminUserId);

    PaymentEntity reject(Long paymentId, String reason, Long adminUserId);

    MemberPaymentView startForMember(StartMemberPaymentCommand command, Requester requester);

    List<MemberPaymentView> listForMember(Requester requester);

    MemberPaymentView getForMember(Long paymentId, Requester requester);

    MemberPaymentView cancelForMember(Long paymentId, Requester requester);

    MemberPaymentView rescheduleForMember(Long paymentId, LocalDateTime appointmentAt, Requester requester);

    void handleGatewayNotification(GatewayNotification notification);
}
