package de.nailsbeauty.whatsappgateway.web;

import de.nailsbeauty.whatsappgateway.dto.request.SendTemplateMessageRequest;
import de.nailsbeauty.whatsappgateway.dto.request.SendTextMessageRequest;
import de.nailsbeauty.whatsappgateway.dto.response.MessageSendResponse;
import de.nailsbeauty.whatsappgateway.dto.response.SubscriptionResponse;
import de.nailsbeauty.whatsappgateway.service.MessagingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-geschuetzte Endpoints zum Versenden von Nachrichten und zum WABA-App-Abo.
 */
@RestController
@RequestMapping("/api/studios/{studioId}")
@RequiredArgsConstructor
public class MessageController {

    private final MessagingService messagingService;

    /**
     * Versendet eine Textnachricht im Namen des Studios.
     *
     * @param studioId fachliche Studio-Kennung
     * @param request  validierter Sende-Request
     * @return Versand-Bestaetigung mit Message-ID
     */
    @PostMapping("/messages")
    public MessageSendResponse sendText(@PathVariable String studioId,
                                        @Valid @RequestBody SendTextMessageRequest request) {
        return messagingService.sendText(studioId, request.to(), request.text());
    }

    /**
     * Versendet eine Template-Nachricht im Namen des Studios.
     *
     * @param studioId fachliche Studio-Kennung
     * @param request  validierter Template-Sende-Request
     * @return Versand-Bestaetigung mit Message-ID
     */
    @PostMapping("/messages/template")
    public MessageSendResponse sendTemplate(@PathVariable String studioId,
                                            @Valid @RequestBody SendTemplateMessageRequest request) {
        return messagingService.sendTemplate(
                studioId, request.to(), request.templateName(), request.languageCode());
    }

    /**
     * Abonniert die App auf die WABA des Studios ({@code subscribed_apps}).
     *
     * @param studioId fachliche Studio-Kennung
     * @return Subscription-Bestaetigung
     */
    @PostMapping("/subscribe")
    public SubscriptionResponse subscribe(@PathVariable String studioId) {
        return messagingService.subscribeApp(studioId);
    }
}
