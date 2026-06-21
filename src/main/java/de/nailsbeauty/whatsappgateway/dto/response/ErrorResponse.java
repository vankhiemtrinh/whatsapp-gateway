package de.nailsbeauty.whatsappgateway.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Einheitliches Fehler-Response-DTO.
 *
 * @param timestamp Zeitpunkt des Fehlers
 * @param status    HTTP-Statuscode
 * @param error     HTTP-Reason-Phrase
 * @param message   menschenlesbare Fehlerbeschreibung
 * @param path      angefragter Pfad
 * @param details   optionale Feld-Details (z. B. Validierungsfehler), kann {@code null} sein
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {

    /**
     * Bequemer Konstruktor ohne Detail-Liste.
     *
     * @param timestamp Zeitpunkt des Fehlers
     * @param status    HTTP-Statuscode
     * @param error     HTTP-Reason-Phrase
     * @param message   menschenlesbare Fehlerbeschreibung
     * @param path      angefragter Pfad
     */
    public ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
        this(timestamp, status, error, message, path, null);
    }
}
