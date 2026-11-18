package com.afcgym.platform.web.controllers;

import com.afcgym.platform.mapper.PaymentMapper;
import com.afcgym.platform.service.IPaymentService;
import com.afcgym.platform.service.dto.RenewMembershipCommand;
import com.afcgym.platform.web.dto.request.RenewMembershipRequest;
import com.afcgym.platform.web.dto.response.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Payments")
public class PaymentsController {

    private final IPaymentService paymentService;
    private final PaymentMapper paymentMapper;

    @PostMapping("/v1/members/{memberId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Renew a member's membership with a payment")
    public PaymentResponse renew(
            @PathVariable Long memberId,
            @Valid @RequestBody RenewMembershipRequest request) {
        return paymentMapper.toResponse(paymentService.renew(memberId, new RenewMembershipCommand(
                request.membershipType(),
                request.method(),
                request.cardLastFour(),
                request.scheduledDate())));
    }

    @GetMapping("/v1/members/{memberId}/payments")
    @Operation(summary = "List a member's payment and renewal history")
    public List<PaymentResponse> listByMember(@PathVariable Long memberId) {
        return paymentService.listByMember(memberId).stream().map(paymentMapper::toResponse).toList();
    }

    @PatchMapping("/v1/payments/{id}/approve")
    @Operation(summary = "Approve a pending SPEI or cash payment")
    public PaymentResponse approve(@PathVariable Long id) {
        return paymentMapper.toResponse(paymentService.approve(id));
    }
}
