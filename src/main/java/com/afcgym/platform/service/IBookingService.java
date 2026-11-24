package com.afcgym.platform.service;

import com.afcgym.platform.persistance.entities.BookingEntity;
import com.afcgym.platform.service.dto.BookClassCommand;
import com.afcgym.platform.service.dto.Requester;
import java.util.List;

public interface IBookingService {

    BookingEntity book(BookClassCommand command, Requester requester);

    BookingEntity cancel(Long id, Requester requester);

    List<BookingEntity> listUpcoming(Requester requester);
}
