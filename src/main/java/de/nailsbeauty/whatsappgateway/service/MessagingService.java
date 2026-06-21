package de.nailsbeauty.whatsappgateway.service;

import de.nailsbeauty.whatsappgateway.domain.StudioConfig;
import de.nailsbeauty.whatsappgateway.dto.response.MessageSendResponse;
import de.nailsbeauty.whatsappgateway.dto.response.SubscriptionResponse;
import de.nailsbeauty.whatsappgateway.exception.ConflictException;
import de.nailsbeauty.whatsappgateway.whatsapp.WhatsAppClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestriert das Versenden von Nachrichten und das WABA-App-Abo fuer ein konkretes Studio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessagingService {

    private final StudioConfigService studioConfigService;
    private final WhatsAppClient whatsAppClient;

    /**
     * Versendet eine Textnachricht im Namen eines Studios.
     *
     * @param studioId fachliche Studio-Kennung
     * @param to       Empfaenger-Telefonnummer
     * @param text     Nachrichtentext
     * @return Versand-Bestaetigung mit Message-ID
     * @throws de.nailsbeauty.whatsappgateway.exception.NotFoundException     wenn das Studio fehlt
     * @throws ConflictException                                            wenn das Studio inaktiv ist
     * @throws de.nailsbeauty.whatsappgateway.exception.WhatsAppApiException   bei Graph-API-Fehlern
     */
    @Transactional(readOnly = true)
    public MessageSendResponse sendText(String studioId, String to, String text) {
        var studio = requireActiveStudio(studioId);
        var messageId = whatsAppClient.sendText(studio, to, text);
        log.info("Textnachricht gesendet: studioId={}, to={}, messageId={}", studioId, to, messageId);
        return new MessageSendResponse(messageId, to);
    }

    /**
     * Versendet eine Template-Nachricht im Namen eines Studios.
     *
     * @param studioId     fachliche Studio-Kennung
     * @param to           Empfaenger-Telefonnummer
     * @param templateName Name des genehmigten Templates
     * @param languageCode Sprachcode des Templates
     * @param bodyParams   positionsbezogene Body-Parameter ({@code null}/leer = ohne Parameter)
     * @return Versand-Bestaetigung mit Message-ID
     * @throws de.nailsbeauty.whatsappgateway.exception.NotFoundException     wenn das Studio fehlt
     * @throws ConflictException                                            wenn das Studio inaktiv ist
     * @throws de.nailsbeauty.whatsappgateway.exception.WhatsAppApiException   bei Graph-API-Fehlern
     */
    @Transactional(readOnly = true)
    public MessageSendResponse sendTemplate(String studioId, String to, String templateName,
                                            String languageCode, java.util.List<String> bodyParams) {
        var studio = requireActiveStudio(studioId);
        var messageId = whatsAppClient.sendTemplate(studio, to, templateName, languageCode, bodyParams);
        log.info("Template-Nachricht gesendet: studioId={}, to={}, template={}, messageId={}",
                studioId, to, templateName, messageId);
        return new MessageSendResponse(messageId, to);
    }

    /**
     * Abonniert die App auf die WABA eines Studios.
     *
     * @param studioId fachliche Studio-Kennung
     * @return Subscription-Bestaetigung
     * @throws de.nailsbeauty.whatsappgateway.exception.NotFoundException     wenn das Studio fehlt
     * @throws de.nailsbeauty.whatsappgateway.exception.WhatsAppApiException   bei Graph-API-Fehlern
     */
    @Transactional(readOnly = true)
    public SubscriptionResponse subscribeApp(String studioId) {
        var studio = studioConfigService.getEntity(studioId);
        var success = whatsAppClient.subscribeApp(studio);
        log.info("WABA-App-Abo fuer studioId={} (wabaId={}): success={}",
                studioId, studio.getWabaId(), success);
        return new SubscriptionResponse(studio.getWabaId(), success);
    }

    /** Laedt das Studio und stellt sicher, dass es aktiv ist. */
    private StudioConfig requireActiveStudio(String studioId) {
        var studio = studioConfigService.getEntity(studioId);
        if (!studio.isActive()) {
            throw new ConflictException("Studio ist inaktiv: " + studioId);
        }
        return studio;
    }
}
