package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.PaymentEntity;
import com.afcgym.platform.service.dto.GatewayCheckout;
import com.afcgym.platform.service.dto.GatewayPayment;

public interface IPaymentGatewayService {

    boolean isConfigured();

    GatewayCheckout createCheckout(PaymentEntity payment);

    boolean isSignatureValid(String dataId, String requestId, String signature);

    GatewayPayment fetchPayment(String gatewayPaymentId);
}
