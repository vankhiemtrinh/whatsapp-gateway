package de.nailsbeauty.whatsappgateway.dto.response;

/**
 * Response-DTO nach erfolgreichem Versand einer Nachricht.
 *
 * @param messageId von Meta vergebene Nachrichten-ID ({@code wamid...})
 * @param to        Empfaenger-Telefonnummer
 */
public record MessageSendResponse(
        String messageId,
        String to
) {
}
