package com.afcgym.platform.service.impl;

import com.afcgym.platform.service.IPushDeliveryService;
import com.afcgym.platform.service.dto.ExpoPushResponse;
import com.afcgym.platform.service.dto.ExpoPushTicket;
import com.afcgym.platform.service.dto.PushDeliveryReport;
import com.afcgym.platform.service.dto.PushMessage;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpoPushDeliveryServiceImpl implements IPushDeliveryService {

    private static final int MAX_MESSAGES_PER_REQUEST = 100;

    @Qualifier("expoPushRestClient")
    private final RestClient expoPushRestClient;

    @Override
    public PushDeliveryReport deliver(List<PushMessage> messages) {
        Set<String> acceptedTokens = new HashSet<>();
        Set<String> unregisteredTokens = new HashSet<>();
        for (int start = 0; start < messages.size(); start += MAX_MESSAGES_PER_REQUEST) {
            List<PushMessage> batch = messages.subList(start, Math.min(start + MAX_MESSAGES_PER_REQUEST, messages.size()));
            List<ExpoPushTicket> tickets = send(batch);
            for (int index = 0; index < Math.min(batch.size(), tickets.size()); index++) {
                String token = batch.get(index).to();
                ExpoPushTicket ticket = tickets.get(index);
                if (ticket.accepted()) {
                    acceptedTokens.add(token);
                } else if (ticket.deviceNotRegistered()) {
                    unregisteredTokens.add(token);
                }
            }
        }
        return new PushDeliveryReport(acceptedTokens, unregisteredTokens);
    }

    private List<ExpoPushTicket> send(List<PushMessage> batch) {
        try {
            ExpoPushResponse response = expoPushRestClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(batch)
                    .retrieve()
                    .body(ExpoPushResponse.class);
            if (response == null || response.data() == null) {
                return List.of();
            }
            return response.data();
        } catch (RestClientException exception) {
            log.warn("Expo push request failed: {}", exception.getClass().getSimpleName());
            return List.of();
        }
    }
}
