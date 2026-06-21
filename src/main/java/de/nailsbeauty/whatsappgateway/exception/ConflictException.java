package de.nailsbeauty.whatsappgateway.exception;

/**
 * Wird geworfen, wenn eine Anlage gegen eine Eindeutigkeitsregel verstoesst (HTTP 409).
 */
public class ConflictException extends RuntimeException {

    /**
     * Erstellt die Exception mit einer Fehlermeldung.
     *
     * @param message beschreibende Fehlermeldung
     */
    public ConflictException(String message) {
        super(message);
    }
}
