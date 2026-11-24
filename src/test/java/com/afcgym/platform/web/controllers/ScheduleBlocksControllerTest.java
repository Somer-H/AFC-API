package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.afcgym.platform.persistance.entities.enums.Role;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduleBlocksControllerTest {

    private static final String MONDAY_MORNING =
            "{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"07:00\",\"endTime\":\"08:00\",\"maxCapacity\":20}";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void givenASocioToken_whenCreateBlock_thenReturnForbidden() throws Exception {
        mockMvc.perform(post("/v1/schedule-blocks")
                        .with(asRole(Role.SOCIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MONDAY_MORNING))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenACoach_whenCreateBlock_thenReturnItWithTheCoachAndTheDerivedShift() throws Exception {
        long coachId = readId(mockMvc.perform(post("/v1/coaches")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alex Rivera\"}"))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/v1/schedule-blocks")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"18:00\",\"endTime\":\"19:00\","
                                + "\"maxCapacity\":15,\"coachId\":" + coachId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.startTime").value("18:00:00"))
                .andExpect(jsonPath("$.shift").value("EVENING"))
                .andExpect(jsonPath("$.maxCapacity").value(15))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.coach.name").value("Alex Rivera"));
    }

    @Test
    void givenBlocksOnSeveralDays_whenListByDay_thenReturnOnlyThatDaySortedByStartTime() throws Exception {
        createBlock("{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"18:00\",\"endTime\":\"19:00\",\"maxCapacity\":10}");
        createBlock(MONDAY_MORNING);
        createBlock("{\"dayOfWeek\":\"TUESDAY\",\"startTime\":\"07:00\",\"endTime\":\"08:00\",\"maxCapacity\":10}");

        mockMvc.perform(get("/v1/schedule-blocks").param("dayOfWeek", "MONDAY").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].shift").value("MORNING"))
                .andExpect(jsonPath("$[1].shift").value("EVENING"));
    }

    @Test
    void givenAZeroCapacity_whenCreateBlock_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/schedule-blocks")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"07:00\",\"endTime\":\"08:00\",\"maxCapacity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenAnEndBeforeTheStart_whenCreateBlock_thenReturnBadRequestWithAClearMessage() throws Exception {
        mockMvc.perform(post("/v1/schedule-blocks")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":\"MONDAY\",\"startTime\":\"09:00\",\"endTime\":\"08:00\",\"maxCapacity\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La hora de inicio debe ser anterior a la hora de fin"));
    }

    @Test
    void givenABlock_whenUpdateDeactivateAndActivate_thenEachChangeIsReturned() throws Exception {
        long blockId = createBlock(MONDAY_MORNING);

        mockMvc.perform(put("/v1/schedule-blocks/" + blockId)
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":\"WEDNESDAY\",\"startTime\":\"06:00\",\"endTime\":\"07:30\",\"maxCapacity\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayOfWeek").value("WEDNESDAY"))
                .andExpect(jsonPath("$.endTime").value("07:30:00"))
                .andExpect(jsonPath("$.maxCapacity").value(25));

        mockMvc.perform(patch("/v1/schedule-blocks/" + blockId + "/deactivate").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(patch("/v1/schedule-blocks/" + blockId + "/activate").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void givenAnUnknownId_whenGetBlock_thenReturnNotFound() throws Exception {
        mockMvc.perform(get("/v1/schedule-blocks/999999").with(asRole(Role.ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenABlockWithoutBookings_whenOccupancyByDayOfWeek_thenItShowsZeroPercent() throws Exception {
        createBlock(MONDAY_MORNING);

        mockMvc.perform(get("/v1/schedule-blocks/occupancy").param("dayOfWeek", "MONDAY").with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.blocks.length()").value(1))
                .andExpect(jsonPath("$.blocks[0].booked").value(0))
                .andExpect(jsonPath("$.blocks[0].available").value(20))
                .andExpect(jsonPath("$.blocks[0].occupancyPercentage").value(0.0));
    }

    @Test
    void givenASocioToken_whenListCoaches_thenReturnForbidden() throws Exception {
        mockMvc.perform(get("/v1/coaches").with(asRole(Role.SOCIO)))
                .andExpect(status().isForbidden());
    }

    private long createBlock(String body) throws Exception {
        return readId(mockMvc.perform(post("/v1/schedule-blocks")
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
