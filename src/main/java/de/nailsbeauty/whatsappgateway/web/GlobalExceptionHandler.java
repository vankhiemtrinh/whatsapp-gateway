package de.nailsbeauty.whatsappgateway.web;

import de.nailsbeauty.whatsappgateway.dto.response.ErrorResponse;
import de.nailsbeauty.whatsappgateway.exception.ConflictException;
import de.nailsbeauty.whatsappgateway.exception.NotFoundException;
import de.nailsbeauty.whatsappgateway.exception.WhatsAppApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Zentrales Exception-Handling: bildet Exceptions auf einheitliche {@link ErrorResponse}-DTOs ab.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Behandelt fehlende Ressourcen (HTTP 404).
     *
     * @param ex      die Exception
     * @param request der HTTP-Request
     * @return Fehlerantwort mit Status 404
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    /**
     * Behandelt Eindeutigkeits-/Zustandskonflikte (HTTP 409).
     *
     * @param ex      die Exception
     * @param request der HTTP-Request
     * @return Fehlerantwort mit Status 409
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    /**
     * Behandelt Validierungsfehler von Request-DTOs (HTTP 400).
     *
     * @param ex      die Validierungs-Exception
     * @param request der HTTP-Request
     * @return Fehlerantwort mit Status 400 inkl. Feld-Details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validierung fehlgeschlagen", request, details);
    }

    /**
     * Behandelt Fehler beim Aufruf der Meta Graph API (HTTP 502).
     *
     * @param ex      die Upstream-Exception
     * @param request der HTTP-Request
     * @return Fehlerantwort mit Status 502
     */
    @ExceptionHandler(WhatsAppApiException.class)
    public ResponseEntity<ErrorResponse> handleWhatsAppApi(WhatsAppApiException ex, HttpServletRequest request) {
        log.error("Upstream-Fehler bei der Graph API: {}", ex.getMessage());
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), request, null);
    }

    /**
     * Auffangnetz fuer unerwartete Fehler (HTTP 500).
     *
     * @param ex      die Exception
     * @param request der HTTP-Request
     * @return generische Fehlerantwort mit Status 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unerwarteter Fehler bei {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Interner Serverfehler", request, null);
    }

    /** Baut eine einheitliche Fehlerantwort. */
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message,
                                                HttpServletRequest request, List<String> details) {
        var body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                details);
        return ResponseEntity.status(status).body(body);
    }
}
