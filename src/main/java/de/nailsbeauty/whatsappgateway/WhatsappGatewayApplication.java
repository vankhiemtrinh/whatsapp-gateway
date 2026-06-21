package de.nailsbeauty.whatsappgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Einstiegspunkt der whatsapp-gateway-Anwendung.
 *
 * <p>Multi-Tenant-Backend, das als technischer Betreiber (Tech Provider) WhatsApp-Cloud-API-
 * Nachrichten fuer mehrere Studios empfaengt und sendet.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class WhatsappGatewayApplication {

    /**
     * Startet den Spring-Boot-Anwendungskontext.
     *
     * @param args Kommandozeilenargumente, an Spring Boot durchgereicht
     */
    public static void main(String[] args) {
        SpringApplication.run(WhatsappGatewayApplication.class, args);
    }
}
