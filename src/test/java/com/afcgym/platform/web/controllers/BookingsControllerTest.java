package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static com.afcgym.platform.configuration.security.TestAuthentication.asUser;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.afcgym.platform.persistance.entities.enums.Role;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingsControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private LocalDate nextWeek;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        nextWeek = LocalDate.now().plusDays(7);
    }

    @Test
    void givenASocioAccount_whenBookTheirOwnSpot_thenTheOccupancyCountsIt() throws Exception {
        long memberId = createMember("Ana Socia");
        long userId = createAccount(memberId, "ana.socia");
        long blockId = createBlock(2);

        book(userId, blockId, nextWeek)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value(memberId))
                .andExpect(jsonPath("$.scheduleBlockId").value(blockId))
                .andExpect(jsonPath("$.classDate").value(nextWeek.toString()))
                .andExpect(jsonPath("$.startTime").value("07:00:00"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/v1/schedule-blocks/occupancy")
                        .param("date", nextWeek.toString())
                        .with(asUser(Role.SOCIO, userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocks[0].block.id").value(blockId))
                .andExpect(jsonPath("$.blocks[0].booked").value(1))
                .andExpect(jsonPath("$.blocks[0].available").value(1))
                .andExpect(jsonPath("$.blocks[0].occupancyPercentage").value(50.0));
    }

    @Test
    void givenAFullClass_whenAnotherSocioBooks_thenReturnConflict() throws Exception {
        long blockId = createBlock(1);
        long firstUser = createAccount(createMember("Ana Socia"), "ana.socia");
        long secondUser = createAccount(createMember("Luis Socio"), "luis.socio");
        book(firstUser, blockId, nextWeek).andExpect(status().isCreated());

        book(secondUser, blockId, nextWeek)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La clase ya alcanzó su cupo máximo"));
    }

    @Test
    void givenABooking_whenTheSameSocioBooksAgain_thenReturnConflict() throws Exception {
        long blockId = createBlock(5);
        long userId = createAccount(createMember("Ana Socia"), "ana.socia");
        book(userId, blockId, nextWeek).andExpect(status().isCreated());

        book(userId, blockId, nextWeek).andExpect(status().isConflict());
    }

    @Test
    void givenAFullClass_whenTheOwnerCancels_thenTheSpotIsFreedRightAway() throws Exception {
        long blockId = createBlock(1);
        long ownerUser = createAccount(createMember("Ana Socia"), "ana.socia");
        long otherUser = createAccount(createMember("Luis Socio"), "luis.socio");
        long bookingId = readId(book(ownerUser, blockId, nextWeek).andReturn().getResponse().getContentAsString());

        mockMvc.perform(patch("/v1/bookings/" + bookingId + "/cancel").with(asUser(Role.SOCIO, otherUser)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Solo puedes cancelar tus propias reservas"));

        mockMvc.perform(patch("/v1/bookings/" + bookingId + "/cancel").with(asUser(Role.SOCIO, ownerUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        book(otherUser, blockId, nextWeek).andExpect(status().isCreated());
    }

    @Test
    void givenABooking_whenAnAdminCancelsIt_thenItIsCancelled() throws Exception {
        long blockId = createBlock(3);
        long userId = createAccount(createMember("Ana Socia"), "ana.socia");
        long bookingId = readId(book(userId, blockId, nextWeek).andReturn().getResponse().getContentAsString());

        mockMvc.perform(patch("/v1/bookings/" + bookingId + "/cancel").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(patch("/v1/bookings/" + bookingId + "/cancel").with(asRole(Role.ADMIN)))
                .andExpect(status().isConflict());
    }

    @Test
    void givenADeactivatedBlock_whenBook_thenRejectItButKeepTheExistingBookings() throws Exception {
        long blockId = createBlock(5);
        long firstUser = createAccount(createMember("Ana Socia"), "ana.socia");
        long secondUser = createAccount(createMember("Luis Socio"), "luis.socio");
        book(firstUser, blockId, nextWeek).andExpect(status().isCreated());

        mockMvc.perform(patch("/v1/schedule-blocks/" + blockId + "/deactivate").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk());

        book(secondUser, blockId, nextWeek)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Este horario está desactivado y no acepta reservas"));

        mockMvc.perform(get("/v1/bookings/me").with(asUser(Role.SOCIO, firstUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].scheduleBlockId").value(blockId));
    }

    @Test
    void givenADateOnAnotherWeekday_whenBook_thenReturnBadRequest() throws Exception {
        long blockId = createBlock(5);
        long userId = createAccount(createMember("Ana Socia"), "ana.socia");

        book(userId, blockId, nextWeek.plusDays(1)).andExpect(status().isBadRequest());
    }

    @Test
    void givenAnAdmin_whenBookWithoutMemberId_thenReturnBadRequestAndWithItCreated() throws Exception {
        long blockId = createBlock(5);
        long memberId = createMember("Ana Socia");

        mockMvc.perform(post("/v1/bookings")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(blockId, nextWeek, null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/v1/bookings")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(blockId, nextWeek, memberId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value(memberId));
    }

    @Test
    void givenASocio_whenBookForAnotherMember_thenReturnForbidden() throws Exception {
        long blockId = createBlock(5);
        long userId = createAccount(createMember("Ana Socia"), "ana.socia");
        long otherMember = createMember("Luis Socio");

        mockMvc.perform(post("/v1/bookings")
                        .with(asUser(Role.SOCIO, userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingBody(blockId, nextWeek, otherMember)))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAnAdminToken_whenListMine_thenReturnForbidden() throws Exception {
        mockMvc.perform(get("/v1/bookings/me").with(asRole(Role.ADMIN)))
                .andExpect(status().isForbidden());
    }

    private ResultActions book(long userId, long blockId, LocalDate classDate) throws Exception {
        return mockMvc.perform(post("/v1/bookings")
                .with(asUser(Role.SOCIO, userId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(blockId, classDate, null)));
    }

    private String bookingBody(long blockId, LocalDate classDate, Long memberId) {
        return "{\"scheduleBlockId\":" + blockId + ",\"classDate\":\"" + classDate + "\""
                + (memberId != null ? ",\"memberId\":" + memberId : "") + "}";
    }

    private long createBlock(int maxCapacity) throws Exception {
        String body = "{\"dayOfWeek\":\"" + nextWeek.getDayOfWeek() + "\",\"startTime\":\"07:00\","
                + "\"endTime\":\"08:00\",\"maxCapacity\":" + maxCapacity + "}";
        return readId(mockMvc.perform(post("/v1/schedule-blocks")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private long createMember(String fullName) throws Exception {
        return readId(mockMvc.perform(post("/v1/members")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"" + fullName + "\",\"phone\":\"5512345678\",\"membershipType\":\"MONTHLY\"}"))
                .andReturn().getResponse().getContentAsString());
    }

    private long createAccount(long memberId, String username) throws Exception {
        String body = "{\"username\":\"" + username + "\",\"email\":\"" + username + "@afcgym.com\","
                + "\"password\":\"Secret-123\"}";
        return readId(mockMvc.perform(post("/v1/members/" + memberId + "/account")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
    }

    private long readId(String response) {
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
}
