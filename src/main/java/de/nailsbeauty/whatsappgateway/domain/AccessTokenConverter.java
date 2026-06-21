package de.nailsbeauty.whatsappgateway.domain;

import de.nailsbeauty.whatsappgateway.config.AppProperties;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * JPA-Converter, der das {@code accessToken}-Feld at-rest mit AES-256-GCM verschluesselt.
 *
 * <p>Beim Persistieren wird ein zufaelliger 12-Byte-IV erzeugt, dem Chiffrat vorangestellt und
 * das Ganze Base64-kodiert gespeichert. Beim Laden wird entsprechend entschluesselt. Der Filter
 * ist als Spring-Bean ausgelegt, damit der Schluessel aus {@link AppProperties} injiziert werden
 * kann (Hibernate nutzt den Spring-Bean-Container fuer Converter).
 */
@Slf4j
@Component
@Converter
public class AccessTokenConverter implements AttributeConverter<String, String> {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Initialisiert den Converter mit dem konfigurierten AES-Schluessel.
     *
     * @param appProperties App-Konfiguration mit Base64-kodiertem 32-Byte-Schluessel
     */
    public AccessTokenConverter(AppProperties appProperties) {
        var key = Base64.getDecoder().decode(appProperties.encryption().key());
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IllegalStateException(
                    "APP_ENCRYPTION_KEY muss Base64 von 16, 24 oder 32 Byte sein (AES-128/192/256)");
        }
        this.keySpec = new SecretKeySpec(key, "AES");
    }

    /**
     * Verschluesselt den Klartext-Token vor dem Speichern.
     *
     * @param attribute Klartext-Access-Token (darf {@code null} sein)
     * @return Base64(IV || Chiffrat) oder {@code null}, falls Eingabe {@code null}
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            var iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            var cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            var combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            // Keine Token-Inhalte loggen.
            throw new IllegalStateException("Verschluesselung des Access-Tokens fehlgeschlagen", e);
        }
    }

    /**
     * Entschluesselt den gespeicherten Token beim Laden.
     *
     * @param dbData Base64(IV || Chiffrat) aus der DB (darf {@code null} sein)
     * @return Klartext-Access-Token oder {@code null}, falls Eingabe {@code null}
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            var combined = Base64.getDecoder().decode(dbData);
            var iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            var cipherText = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);

            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Entschluesselung des Access-Tokens fehlgeschlagen", e);
        }
    }
}
