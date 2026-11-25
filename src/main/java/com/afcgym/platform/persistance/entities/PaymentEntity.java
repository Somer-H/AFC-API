package com.afcgym.platform.persistance.entities;

import com.afcgym.platform.persistance.entities.enums.MembershipType;
import com.afcgym.platform.persistance.entities.enums.PaymentMethod;
import com.afcgym.platform.persistance.entities.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type", nullable = false, length = 20)
    private MembershipType membershipType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    @Setter(AccessLevel.PRIVATE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "discount_percent", nullable = false)
    private int discountPercent;

    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "approved_at")
    private Instant approvedAt;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "approved_by")
    private Long approvedBy;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "rejected_by")
    private Long rejectedBy;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "gateway_preference_id", length = 100)
    private String gatewayPreferenceId;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "gateway_payment_id", length = 30)
    private String gatewayPaymentId;

    @Setter(AccessLevel.PRIVATE)
    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;

    public void approve(Instant approvedAt, Long approvedBy) {
        setStatus(PaymentStatus.APPROVED);
        setApprovedAt(approvedAt);
        setApprovedBy(approvedBy);
    }

    public void approveFromGateway(Instant approvedAt, String gatewayPaymentId) {
        setGatewayPaymentId(gatewayPaymentId);
        approve(approvedAt, null);
    }

    public void markGatewayRejected(String gatewayPaymentId, String detail) {
        setStatus(PaymentStatus.GATEWAY_REJECTED);
        setGatewayPaymentId(gatewayPaymentId);
        setRejectionReason(detail);
    }

    public void attachCheckout(String preferenceId, String checkoutUrl) {
        setGatewayPreferenceId(preferenceId);
        setCheckoutUrl(checkoutUrl);
    }

    public void reject(Instant rejectedAt, Long rejectedBy, String reason) {
        setStatus(PaymentStatus.REJECTED);
        setRejectedAt(rejectedAt);
        setRejectedBy(rejectedBy);
        setRejectionReason(reason);
    }

    public void cancel() {
        setStatus(PaymentStatus.CANCELLED);
    }

    public void awaitAppointmentConfirmation() {
        setStatus(PaymentStatus.PENDING_APPOINTMENT);
    }

    public void confirmAppointment() {
        setStatus(PaymentStatus.APPOINTMENT_CONFIRMED);
    }

    public void markAppointmentCancelled() {
        setStatus(PaymentStatus.APPOINTMENT_CANCELLED);
    }

    public boolean isApproved() {
        return status == PaymentStatus.APPROVED;
    }

    public boolean isPaidThroughGateway() {
        return gatewayPreferenceId != null;
    }
}
