package com.afcgym.platform.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.afcgym.platform.service.dto.PushDeliveryReport;
import com.afcgym.platform.service.dto.PushMessage;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ExpoPushDeliveryServiceImplTest {

    private static final String PUSH_URL = "https://push.test/--/api/v2/push/send";
    private static final String OK_TICKET = "{\"status\":\"ok\",\"id\":\"ticket\"}";

    private MockRestServiceServer server;

    private ExpoPushDeliveryServiceImpl deliveryService;

    @BeforeEach
    void init() {
        RestClient.Builder builder = RestClient.builder().baseUrl(PUSH_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        deliveryService = new ExpoPushDeliveryServiceImpl(builder.build());
    }

    @Test
    void givenExpoTicketsForEachDevice_whenDeliver_thenReportAcceptedAndUnregisteredDevices() {
        server.expect(requestTo(PUSH_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].to").value("ExponentPushToken[ok]"))
                .andExpect(jsonPath("$[0].title").value("Título"))
                .andExpect(jsonPath("$[0].sound").value("default"))
                .andExpect(jsonPath("$[0].data.type").value("EXPIRY_REMINDER"))
                .andRespond(withSuccess("{\"data\":[" + OK_TICKET + ","
                        + "{\"status\":\"error\",\"message\":\"gone\",\"details\":{\"error\":\"DeviceNotRegistered\"}},"
                        + "{\"status\":\"error\",\"message\":\"slow down\",\"details\":{\"error\":\"MessageRateExceeded\"}}]}",
                        MediaType.APPLICATION_JSON));

        PushDeliveryReport report = deliveryService.deliver(List.of(
                message("ExponentPushToken[ok]"), message("ExponentPushToken[gone]"), message("ExponentPushToken[busy]")));

        server.verify();
        assertEquals(Set.of("ExponentPushToken[ok]"), report.acceptedTokens());
        assertEquals(Set.of("ExponentPushToken[gone]"), report.unregisteredTokens());
    }

    @Test
    void givenMoreThanAHundredMessages_whenDeliver_thenSendThemInBatchesOfAHundred() {
        server.expect(requestTo(PUSH_URL))
                .andExpect(jsonPath("$.length()").value(100))
                .andRespond(withSuccess(okTickets(100), MediaType.APPLICATION_JSON));
        server.expect(requestTo(PUSH_URL))
                .andExpect(jsonPath("$.length()").value(50))
                .andRespond(withSuccess(okTickets(50), MediaType.APPLICATION_JSON));
        List<PushMessage> messages = IntStream.range(0, 150)
                .mapToObj(index -> message("ExponentPushToken[device-" + index + "]"))
                .toList();

        PushDeliveryReport report = deliveryService.deliver(messages);

        server.verify();
        assertEquals(150, report.acceptedTokens().size());
    }

    @Test
    void givenExpoIsDown_whenDeliver_thenReportNothingAcceptedWithoutFailing() {
        server.expect(requestTo(PUSH_URL)).andRespond(withServerError());

        PushDeliveryReport report = deliveryService.deliver(List.of(message("ExponentPushToken[ok]")));

        server.verify();
        assertTrue(report.acceptedTokens().isEmpty());
        assertTrue(report.unregisteredTokens().isEmpty());
    }

    @Test
    void givenNoMessages_whenDeliver_thenDoNotCallExpo() {
        PushDeliveryReport report = deliveryService.deliver(List.of());

        server.verify();
        assertTrue(report.acceptedTokens().isEmpty());
    }

    private PushMessage message(String token) {
        return new PushMessage(token, "Título", "Cuerpo", "default", Map.of("type", "EXPIRY_REMINDER"));
    }

    private String okTickets(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> OK_TICKET)
                .collect(Collectors.joining(",", "{\"data\":[", "]}"));
    }
}
