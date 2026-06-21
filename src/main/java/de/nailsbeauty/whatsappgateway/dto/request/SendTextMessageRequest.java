package de.nailsbeauty.whatsappgateway.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request-DTO zum Versenden einer Textnachricht.
 *
 * @param to   Empfaenger-Telefonnummer im internationalen Format (z. B. {@code 491701234567})
 * @param text Nachrichtentext (max. 4096 Zeichen gemaess Cloud API)
 */
public record SendTextMessageRequest(
        @NotBlank String to,
        @NotBlank @Size(max = 4096) String text
) {
}
