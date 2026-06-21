package de.nailsbeauty.whatsappgateway.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request-DTO zum Versenden einer Template-Nachricht.
 *
 * @param to           Empfaenger-Telefonnummer im internationalen Format
 * @param templateName Name des im Business Manager genehmigten Templates
 * @param languageCode Sprachcode des Templates, z. B. {@code de} oder {@code en_US}
 */
public record SendTemplateMessageRequest(
        @NotBlank String to,
        @NotBlank String templateName,
        @NotBlank String languageCode
) {
}
