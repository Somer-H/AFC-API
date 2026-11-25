package com.afcgym.platform.web.controllers;

import com.afcgym.platform.configuration.security.AuthenticatedUser;
import com.afcgym.platform.mapper.PaymentAppointmentMapper;
import com.afcgym.platform.service.IPaymentAppointmentService;
import com.afcgym.platform.service.dto.CancelAppointmentCommand;
import com.afcgym.platform.web.dto.request.CancelAppointmentRequest;
import com.afcgym.platform.web.dto.response.AppointmentAgendaResponse;
import com.afcgym.platform.web.dto.response.PaymentAppointmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payment-appointments")
@RequiredArgsConstructor
@Tag(name = "Cash payment appointments")
public class PaymentAppointmentsController {

    private final IPaymentAppointmentService appointmentService;
    private final PaymentAppointmentMapper appointmentMapper;

    @GetMapping
    @Operation(summary = "Agenda for reception: pending requests and upcoming accepted appointments with member and amount")
    public List<AppointmentAgendaResponse> agenda() {
        return appointmentService.listAgenda(LocalDate.now()).stream()
                .map(appointmentMapper::toAgendaResponse)
                .toList();
    }

    @PatchMapping("/{id}/accept")
    @Operation(summary = "Accept a cash payment appointment if it does not overlap another accepted one")
    public PaymentAppointmentResponse accept(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return appointmentMapper.toResponse(appointmentService.accept(id, AuthenticatedUser.fromJwt(jwt).userId()));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a cash payment appointment with an optional reason and suggested time")
    public PaymentAppointmentResponse cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelAppointmentRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return appointmentMapper.toResponse(appointmentService.cancel(id,
                new CancelAppointmentCommand(request.reason(), request.suggestedAt()),
                AuthenticatedUser.fromJwt(jwt).userId()));
    }
}
