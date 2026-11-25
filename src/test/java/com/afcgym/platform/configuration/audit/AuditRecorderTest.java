package com.afcgym.platform.configuration.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.afcgym.platform.persistance.entities.enums.AuditOutcome;
import com.afcgym.platform.persistance.entities.enums.Role;
import com.afcgym.platform.service.IAuditService;
import com.afcgym.platform.service.dto.AuditEntry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

@ExtendWith(MockitoExtension.class)
class AuditRecorderTest {

    @Mock
    private IAuditService auditService;

    @Captor
    private ArgumentCaptor<AuditEntry> entryCaptor;

    private AuditRecorder auditRecorder;

    private HandlerMethod handler;

    @BeforeEach
    void init() throws NoSuchMethodException {
        auditRecorder = new AuditRecorder(auditService);
        handler = new HandlerMethod(new SampleMembersController(), SampleMembersController.class.getMethod("deactivate"));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void givenAnAdminChangeThatSucceeded_whenRecordAdminChange_thenStoreActionActorAndTarget() {
        signIn(Role.ADMIN, 5L);
        MockHttpServletRequest request = request("PATCH", "/v1/members/9/deactivate");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("id", "9"));

        auditRecorder.recordAdminChange(request, handler, 200);

        verify(auditService).record(entryCaptor.capture());
        assertEquals(new AuditEntry(5L, Role.ADMIN, "SampleMembers.deactivate", "PATCH", "/v1/members/9/deactivate",
                "9", AuditOutcome.SUCCESS, 200), entryCaptor.getValue());
    }

    @Test
    void givenACreatedRecord_whenRecordAdminChange_thenUseTheIdOfTheResponseAsTarget() {
        signIn(Role.ADMIN, 5L);
        MockHttpServletRequest request = request("POST", "/v1/members");
        request.setAttribute(AuditRecorder.TARGET_ID_ATTRIBUTE, "42");

        auditRecorder.recordAdminChange(request, handler, 201);

        verify(auditService).record(entryCaptor.capture());
        assertEquals("42", entryCaptor.getValue().targetId());
    }

    @Test
    void givenAReadOrAFailureOrAMember_whenRecordAdminChange_thenStoreNothing() {
        signIn(Role.ADMIN, 5L);
        auditRecorder.recordAdminChange(request("GET", "/v1/members"), handler, 200);
        auditRecorder.recordAdminChange(request("POST", "/v1/members"), handler, 400);
        signIn(Role.SOCIO, 8L);
        auditRecorder.recordAdminChange(request("POST", "/v1/bookings"), handler, 201);

        verifyNoInteractions(auditService);
    }

    @Test
    void givenARequestDeniedBeforeReachingAController_whenRecordDenied_thenStoreItAsAccessDenied() {
        signIn(Role.SOCIO, 8L);

        auditRecorder.recordDenied(request("GET", "/v1/members"), 403);

        verify(auditService).record(entryCaptor.capture());
        assertEquals(new AuditEntry(8L, Role.SOCIO, "AccessDenied", "GET", "/v1/members", null,
                AuditOutcome.DENIED, 403), entryCaptor.getValue());
    }

    @Test
    void givenTheAuditStoreFails_whenRecordDenied_thenDoNotBreakTheResponse() {
        signIn(Role.SOCIO, 8L);
        when(auditService.record(any(AuditEntry.class))).thenThrow(new IllegalStateException("database down"));

        auditRecorder.recordDenied(request("GET", "/v1/members"), 403);

        verify(auditService).record(any(AuditEntry.class));
    }

    private void signIn(Role role, Long userId) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("role", role.name())
                .claim("userId", userId)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRequestURI(path);
        return request;
    }

    static class SampleMembersController {

        public void deactivate() {
        }
    }
}
