package com.afcgym.platform.web.controllers;

import static com.afcgym.platform.configuration.security.TestAuthentication.asRole;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.afcgym.platform.persistance.entities.enums.Role;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RoutinesControllerTest {

    private static final byte[] VIDEO_BYTES = {0, 0, 0, 24, 102, 116, 121, 112, 105, 115, 111, 109};

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    private long coachId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        coachId = readId(mockMvc.perform(post("/v1/coaches")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alex Rivera\"}"))
                .andReturn().getResponse().getContentAsString());
    }

    @Test
    void givenASocioToken_whenCreateRoutine_thenReturnForbidden() throws Exception {
        mockMvc.perform(post("/v1/routines")
                        .with(asRole(Role.SOCIO))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(routine("MONDAY", exercise(null, "Sentadilla"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenAFreeDay_whenCreateRoutine_thenReturnItWithCoachAndOrderedExercises() throws Exception {
        mockMvc.perform(post("/v1/routines")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(routine("MONDAY", exercise(null, "Sentadilla"),
                                "{\"name\":\"Press banca\",\"sets\":3,\"reps\":\"Al fallo\",\"videoUrl\":\"https://youtu.be/abc\"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$.coach.name").value("Alex Rivera"))
                .andExpect(jsonPath("$.exercises.length()").value(2))
                .andExpect(jsonPath("$.exercises[0].name").value("Sentadilla"))
                .andExpect(jsonPath("$.exercises[0].restSeconds").value(90))
                .andExpect(jsonPath("$.exercises[0].videoType").doesNotExist())
                .andExpect(jsonPath("$.exercises[1].reps").value("Al fallo"))
                .andExpect(jsonPath("$.exercises[1].videoType").value("URL"))
                .andExpect(jsonPath("$.exercises[1].videoUrl").value("https://youtu.be/abc"));
    }

    @Test
    void givenADayWithARoutine_whenCreateAnotherOne_thenReturnConflictWithAClearMessage() throws Exception {
        createRoutine("MONDAY");

        mockMvc.perform(post("/v1/routines")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(routine("MONDAY", exercise(null, "Remo"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya hay una rutina para el lunes; edítala o elimínala primero"));
    }

    @Test
    void givenNineExercises_whenCreateRoutine_thenReturnBadRequest() throws Exception {
        String[] nine = new String[9];
        for (int index = 0; index < nine.length; index++) {
            nine[index] = exercise(null, "Ejercicio " + index);
        }

        mockMvc.perform(post("/v1/routines")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(routine("MONDAY", nine)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenNoCoach_whenCreateRoutine_thenReturnBadRequest() throws Exception {
        mockMvc.perform(post("/v1/routines")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dayOfWeek\":\"MONDAY\",\"title\":\"Pierna\",\"exercises\":[" + exercise(null, "Sentadilla") + "]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void givenARoutineOnMonday_whenASocioAsksForMonday_thenReturnIt() throws Exception {
        createRoutine("MONDAY");

        mockMvc.perform(get("/v1/routines/by-day").param("dayOfWeek", "MONDAY").with(asRole(Role.SOCIO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Pierna"))
                .andExpect(jsonPath("$.description").value("Calentar 10 minutos"))
                .andExpect(jsonPath("$.exercises[0].sets").value(4));
    }

    @Test
    void givenNoRoutineOnSunday_whenASocioAsksForSunday_thenSayItClearly() throws Exception {
        mockMvc.perform(get("/v1/routines/by-day").param("dayOfWeek", "SUNDAY").with(asRole(Role.SOCIO)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No hay rutina asignada para el domingo"));
    }

    @Test
    void givenARoutine_whenUpdateKeepingOneExerciseById_thenTheOthersAreReplaced() throws Exception {
        String created = createRoutine("MONDAY");
        long routineId = readId(created);
        long firstExerciseId = ((Number) JsonPath.read(created, "$.exercises[0].id")).longValue();

        mockMvc.perform(put("/v1/routines/" + routineId)
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(routine("TUESDAY", exercise(null, "Zancadas"), exercise(firstExerciseId, "Sentadilla frontal"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayOfWeek").value("TUESDAY"))
                .andExpect(jsonPath("$.exercises.length()").value(2))
                .andExpect(jsonPath("$.exercises[0].name").value("Zancadas"))
                .andExpect(jsonPath("$.exercises[1].id").value(firstExerciseId))
                .andExpect(jsonPath("$.exercises[1].name").value("Sentadilla frontal"));
    }

    @Test
    void givenAnExercise_whenUploadAVideo_thenTheSocioAppCanPlayItWithoutAToken() throws Exception {
        String created = createRoutine("MONDAY");
        long routineId = readId(created);
        long exerciseId = ((Number) JsonPath.read(created, "$.exercises[0].id")).longValue();

        String response = mockMvc.perform(multipart("/v1/routines/" + routineId + "/exercises/" + exerciseId + "/video")
                        .file(new MockMultipartFile("file", "sentadilla.mp4", "video/mp4", VIDEO_BYTES))
                        .with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].videoType").value("FILE"))
                .andExpect(jsonPath("$.exercises[0].videoUrl", matchesPattern("/v1/exercise-videos/[0-9a-f]{32}\\.mp4")))
                .andReturn().getResponse().getContentAsString();
        String videoUrl = JsonPath.read(response, "$.exercises[0].videoUrl");

        mockMvc.perform(get(videoUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType("video/mp4"))
                .andExpect(content().bytes(VIDEO_BYTES));

        mockMvc.perform(get(videoUrl).header(HttpHeaders.RANGE, "bytes=0-3"))
                .andExpect(status().isPartialContent());
    }

    @Test
    void givenAPdf_whenUploadAsVideo_thenReturnBadRequest() throws Exception {
        String created = createRoutine("MONDAY");
        long exerciseId = ((Number) JsonPath.read(created, "$.exercises[0].id")).longValue();

        mockMvc.perform(multipart("/v1/routines/" + readId(created) + "/exercises/" + exerciseId + "/video")
                        .file(new MockMultipartFile("file", "rutina.pdf", "application/pdf", VIDEO_BYTES))
                        .with(asRole(Role.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Solo se aceptan videos MP4, MOV o WEBM"));
    }

    @Test
    void givenAnExerciseWithAVideoUrl_whenRemoveVideo_thenItHasNoVideo() throws Exception {
        String created = createRoutineWithVideoUrl();
        long exerciseId = ((Number) JsonPath.read(created, "$.exercises[0].id")).longValue();

        mockMvc.perform(delete("/v1/routines/" + readId(created) + "/exercises/" + exerciseId + "/video")
                        .with(asRole(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].videoType").doesNotExist())
                .andExpect(jsonPath("$.exercises[0].videoUrl").doesNotExist());
    }

    @Test
    void givenARoutine_whenDelete_thenTheDayIsFreeAgain() throws Exception {
        long routineId = readId(createRoutine("MONDAY"));

        mockMvc.perform(delete("/v1/routines/" + routineId).with(asRole(Role.ADMIN)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/v1/routines/by-day").param("dayOfWeek", "MONDAY").with(asRole(Role.ADMIN)))
                .andExpect(status().isNotFound());
        createRoutine("MONDAY");
    }

    @Test
    void givenAnUnknownVideoName_whenGetVideo_thenReturnNotFound() throws Exception {
        mockMvc.perform(get("/v1/exercise-videos/0123456789abcdef0123456789abcdef.mp4"))
                .andExpect(status().isNotFound());
    }

    private String createRoutine(String dayOfWeek) throws Exception {
        return mockMvc.perform(post("/v1/routines")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(routine(dayOfWeek, exercise(null, "Sentadilla"), exercise(null, "Peso muerto"))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String createRoutineWithVideoUrl() throws Exception {
        return mockMvc.perform(post("/v1/routines")
                        .with(asRole(Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(routine("FRIDAY",
                                "{\"name\":\"Burpees\",\"sets\":3,\"reps\":\"15\",\"videoUrl\":\"https://youtu.be/burpees\"}")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private String routine(String dayOfWeek, String... exercises) {
        return "{\"dayOfWeek\":\"" + dayOfWeek + "\",\"title\":\"Pierna\",\"description\":\"Calentar 10 minutos\","
                + "\"coachId\":" + coachId + ",\"exercises\":[" + String.join(",", exercises) + "]}";
    }

    private String exercise(Long id, String name) {
        return "{" + (id != null ? "\"id\":" + id + "," : "")
                + "\"name\":\"" + name + "\",\"sets\":4,\"reps\":\"10-12\",\"restSeconds\":90}";
    }

    private long readId(String response) {
        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }
}
