package com.afcgym.platform.configuration.audit;

import com.afcgym.platform.configuration.security.AuthenticatedUser;
import com.afcgym.platform.persistance.entities.enums.AuditOutcome;
import com.afcgym.platform.persistance.entities.enums.Role;
import com.afcgym.platform.service.IAuditService;
import com.afcgym.platform.service.dto.AuditEntry;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditRecorder {

    public static final String TARGET_ID_ATTRIBUTE = AuditRecorder.class.getName() + ".targetId";

    private static final Set<String> CHANGING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final String ACCESS_DENIED_ACTION = "AccessDenied";
    private static final String CONTROLLER_SUFFIX = "Controller";
    private static final String PREFERRED_PATH_VARIABLE = "id";

    private final IAuditService auditService;

    public void recordAdminChange(HttpServletRequest request, Object handler, int status) {
        if (!CHANGING_METHODS.contains(request.getMethod()) || status < 200 || status >= 300) {
            return;
        }
        AuthenticatedUser user = currentUser();
        if (user == null || user.role() != Role.ADMIN) {
            return;
        }
        record(request, user, actionOf(handler), AuditOutcome.SUCCESS, status);
    }

    public void recordDenied(HttpServletRequest request, int status) {
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        record(request, currentUser(), handler == null ? ACCESS_DENIED_ACTION : actionOf(handler),
                AuditOutcome.DENIED, status);
    }

    private void record(HttpServletRequest request, AuthenticatedUser user, String action, AuditOutcome outcome,
                        int status) {
        try {
            auditService.record(new AuditEntry(
                    user == null ? null : user.userId(),
                    user == null ? null : user.role(),
                    action,
                    request.getMethod(),
                    request.getRequestURI(),
                    targetIdOf(request),
                    outcome,
                    status));
        } catch (RuntimeException exception) {
            log.error("Could not write the audit entry for {} {}", request.getMethod(), request.getRequestURI(), exception);
        }
    }

    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            return null;
        }
        try {
            return AuthenticatedUser.fromJwt(jwtAuthentication.getToken());
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String actionOf(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return ACCESS_DENIED_ACTION;
        }
        String controller = handlerMethod.getBeanType().getSimpleName();
        if (controller.endsWith(CONTROLLER_SUFFIX)) {
            controller = controller.substring(0, controller.length() - CONTROLLER_SUFFIX.length());
        }
        return controller + "." + handlerMethod.getMethod().getName();
    }

    private String targetIdOf(HttpServletRequest request) {
        Object createdId = request.getAttribute(TARGET_ID_ATTRIBUTE);
        if (createdId != null) {
            return createdId.toString();
        }
        if (!(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?, ?> variables)
                || variables.isEmpty()) {
            return null;
        }
        Object preferred = variables.get(PREFERRED_PATH_VARIABLE);
        return String.valueOf(preferred != null ? preferred : variables.values().iterator().next());
    }
}
