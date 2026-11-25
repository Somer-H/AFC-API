package com.afcgym.platform.web.controllers;

import com.afcgym.platform.configuration.security.AuthenticatedUser;
import com.afcgym.platform.mapper.PaymentMapper;
import com.afcgym.platform.service.IPaymentService;
import com.afcgym.platform.service.dto.Requester;
import com.afcgym.platform.service.dto.StartMemberPaymentCommand;
import com.afcgym.platform.web.dto.request.RescheduleAppointmentRequest;
import com.afcgym.platform.web.dto.request.StartMemberPaymentRequest;
import com.afcgym.platform.web.dto.response.MemberPaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments/me")
@RequiredArgsConstructor
@Tag(name = "Member payments")
public class MemberPaymentsController {

    private final IPaymentService paymentService;
    private final PaymentMapper paymentMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start the renewal of the signed-in member: card (Mercado Pago), bank transfer or cash with an appointment")
    public MemberPaymentResponse start(
            @Valid @RequestBody StartMemberPaymentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return paymentMapper.toMemberResponse(paymentService.startForMember(
                new StartMemberPaymentCommand(request.membershipType(), request.method(), request.appointmentAt()),
                toRequester(jwt)));
    }

    @GetMapping
    @Operation(summary = "Payments of the signed-in member, newest first")
    public List<MemberPaymentResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return paymentService.listForMember(toRequester(jwt)).stream().map(paymentMapper::toMemberResponse).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "One payment of the signed-in member, to follow its status")
    public MemberPaymentResponse getMine(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return paymentMapper.toMemberResponse(paymentService.getForMember(id, toRequester(jwt)));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an unpaid payment of the signed-in member to choose another method")
    public MemberPaymentResponse cancelMine(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return paymentMapper.toMemberResponse(paymentService.cancelForMember(id, toRequester(jwt)));
    }

    @PatchMapping("/{id}/appointment")
    @Operation(summary = "Ask for a new date and time to pay a cash renewal at reception")
    public MemberPaymentResponse reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleAppointmentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return paymentMapper.toMemberResponse(paymentService.rescheduleForMember(
                id, request.appointmentAt(), toRequester(jwt)));
    }

    private Requester toRequester(Jwt jwt) {
        AuthenticatedUser authenticatedUser = AuthenticatedUser.fromJwt(jwt);
        return new Requester(authenticatedUser.userId(), authenticatedUser.role());
    }
}
