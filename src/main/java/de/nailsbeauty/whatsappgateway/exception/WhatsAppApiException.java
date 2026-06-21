package de.nailsbeauty.whatsappgateway.exception;

/**
 * Wird geworfen, wenn ein Aufruf an die Meta Graph API fehlschlaegt (Upstream-Fehler, HTTP 502).
 */
public class WhatsAppApiException extends RuntimeException {

    /**
     * Erstellt die Exception mit Fehlermeldung und Ursache.
     *
     * @param message beschreibende Fehlermeldung
     * @param cause   urspruengliche Ursache
     */
    public WhatsAppApiException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Erstellt die Exception mit einer Fehlermeldung.
     *
     * @param message beschreibende Fehlermeldung
     */
    public WhatsAppApiException(String message) {
        super(message);
    }
}
