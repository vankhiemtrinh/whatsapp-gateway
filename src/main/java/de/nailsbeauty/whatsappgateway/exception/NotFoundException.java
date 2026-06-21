package de.nailsbeauty.whatsappgateway.exception;

/**
 * Wird geworfen, wenn eine angeforderte Ressource nicht existiert (HTTP 404).
 */
public class NotFoundException extends RuntimeException {

    /**
     * Erstellt die Exception mit einer Fehlermeldung.
     *
     * @param message beschreibende Fehlermeldung
     */
    public NotFoundException(String message) {
        super(message);
    }
}
