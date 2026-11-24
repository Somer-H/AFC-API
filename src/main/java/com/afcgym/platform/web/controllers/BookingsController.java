package com.afcgym.platform.web.controllers;

import com.afcgym.platform.configuration.security.AuthenticatedUser;
import com.afcgym.platform.mapper.BookingMapper;
import com.afcgym.platform.service.IBookingService;
import com.afcgym.platform.service.dto.BookClassCommand;
import com.afcgym.platform.service.dto.Requester;
import com.afcgym.platform.web.dto.request.BookClassRequest;
import com.afcgym.platform.web.dto.response.BookingResponse;
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
@RequestMapping("/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings")
public class BookingsController {

    private final IBookingService bookingService;
    private final BookingMapper bookingMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Book a spot in a class: a SOCIO books for themselves, an ADMIN must send memberId")
    public BookingResponse book(@Valid @RequestBody BookClassRequest request, @AuthenticationPrincipal Jwt jwt) {
        return bookingMapper.toResponse(bookingService.book(
                new BookClassCommand(request.scheduleBlockId(), request.classDate(), request.memberId()),
                toRequester(jwt)));
    }

    @GetMapping("/me")
    @Operation(summary = "Upcoming active bookings of the signed-in member")
    public List<BookingResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
        return bookingService.listUpcoming(toRequester(jwt)).stream().map(bookingMapper::toResponse).toList();
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booking: its owner or an ADMIN")
    public BookingResponse cancel(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return bookingMapper.toResponse(bookingService.cancel(id, toRequester(jwt)));
    }

    private Requester toRequester(Jwt jwt) {
        AuthenticatedUser authenticatedUser = AuthenticatedUser.fromJwt(jwt);
        return new Requester(authenticatedUser.userId(), authenticatedUser.role());
    }
}
