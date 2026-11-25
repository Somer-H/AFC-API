package com.afcgym.platform.web.controllers.advice;

import com.afcgym.platform.configuration.audit.AuditRecorder;
import com.afcgym.platform.exception.BadRequestException;
import com.afcgym.platform.exception.ConflictException;
import com.afcgym.platform.exception.ForbiddenException;
import com.afcgym.platform.exception.NotFoundException;
import com.afcgym.platform.exception.OwnershipViolationException;
import com.afcgym.platform.exception.ServiceUnavailableException;
import com.afcgym.platform.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private static final Set<String> REQUIRED_CONSTRAINTS = Set.of("NotNull", "NotBlank", "NotEmpty");
    private static final String REQUIRED = "es obligatorio";

    private final AuditRecorder auditRecorder;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidBody(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new TreeMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), reasonFor(error.getCode()));
        }
        if (fields.isEmpty()) {
            return buildResponse(HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos");
        }
        boolean onlyMissing = fields.values().stream().allMatch(REQUIRED::equals);
        String message = onlyMissing
                ? "Faltan campos obligatorios: " + String.join(", ", fields.keySet())
                : "Revisa estos campos: " + fields.entrySet().stream()
                        .map(entry -> entry.getKey() + " (" + entry.getValue() + ")")
                        .collect(Collectors.joining(", "));
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), message, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadableBody(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getMostSpecificCause();
        if (cause instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
            String field = fieldPath(mismatch);
            Class<?> targetType = mismatch.getTargetType();
            if (targetType != null && targetType.isEnum()) {
                String allowed = Arrays.stream(targetType.getEnumConstants())
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));
                String value = mismatch instanceof InvalidFormatException invalid ? String.valueOf(invalid.getValue()) : "";
                return new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                        "El valor '" + value + "' no es válido para " + field + ". Valores permitidos: " + allowed,
                        Map.of(field, "no es válido"));
            }
            return new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                    "El campo " + field + " no tiene el formato esperado", Map.of(field, "no tiene el formato esperado"));
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es un JSON válido");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                "El parámetro " + exception.getName() + " no tiene un formato válido",
                Map.of(exception.getName(), "no tiene el formato esperado"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParameter(MissingServletRequestParameterException exception) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(),
                "Falta el parámetro obligatorio " + exception.getParameterName(),
                Map.of(exception.getParameterName(), REQUIRED));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "La solicitud hace referencia a datos que no existen o viola una restricción");
    }

    @ExceptionHandler(OwnershipViolationException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleOwnershipViolation(OwnershipViolationException exception, HttpServletRequest request) {
        auditRecorder.recordDenied(request, HttpStatus.NOT_FOUND.value());
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(NotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(BadRequestException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUnauthorized(UnauthorizedException exception) {
        return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbidden(ForbiddenException exception, HttpServletRequest request) {
        auditRecorder.recordDenied(request, HttpStatus.FORBIDDEN.value());
        return buildResponse(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
    public ErrorResponse handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        return buildResponse(HttpStatus.CONTENT_TOO_LARGE, "El archivo supera el tamaño máximo permitido (100 MB)");
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleServiceUnavailable(ServiceUnavailableException exception) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(ConflictException exception) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleAnythingElse(Exception exception, HttpServletResponse response) {
        if (exception instanceof org.springframework.web.ErrorResponse springError) {
            int status = springError.getStatusCode().value();
            response.setStatus(status);
            return new ErrorResponse(status, messageForStatus(status));
        }
        log.error("Unexpected error while handling a request", exception);
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intenta de nuevo más tarde");
    }

    private String reasonFor(String constraint) {
        if (constraint == null) {
            return "no es válido";
        }
        if (REQUIRED_CONSTRAINTS.contains(constraint)) {
            return REQUIRED;
        }
        return switch (constraint) {
            case "Size", "Length" -> "tiene una longitud no permitida";
            case "Pattern" -> "no tiene el formato esperado";
            case "Email" -> "no es un correo válido";
            case "Min", "Max", "DecimalMin", "DecimalMax", "Positive", "PositiveOrZero", "Negative", "Digits" ->
                    "está fuera del rango permitido";
            case "Future", "FutureOrPresent", "Past", "PastOrPresent" -> "tiene una fecha no permitida";
            default -> "no es válido";
        };
    }

    private String fieldPath(JacksonException exception) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference reference : exception.getPath()) {
            if (reference.getPropertyName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        return path.isEmpty() ? "desconocido" : path.toString();
    }

    private String messageForStatus(int status) {
        return switch (status) {
            case 400 -> "Los datos enviados no son válidos";
            case 404 -> "La ruta solicitada no existe";
            case 405 -> "Esta operación no está disponible en esta ruta";
            case 406 -> "No se puede responder en el formato solicitado";
            case 415 -> "El contenido debe enviarse como JSON";
            case 503 -> "El servicio no está disponible por ahora. Intenta de nuevo más tarde";
            default -> "No se pudo completar la solicitud";
        };
    }

    private ErrorResponse buildResponse(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), message);
    }
}
