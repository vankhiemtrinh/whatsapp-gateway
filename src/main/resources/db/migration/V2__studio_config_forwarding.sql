-- V2__studio_config_forwarding.sql
-- Erweitert studio_config um die Weiterleitung eingehender WhatsApp-Nachrichten
-- an die Backend-Instanz des jeweiligen Studios.
--   backend_base_url : Basis-URL des Studio-Backends (ohne Trailing-Slash). NULL/leer
--                      = keine Weiterleitung (Gateway haelt nur Audit/Idempotenz).
--   forward_secret   : geteiltes Geheimnis fuer den Header X-Gateway-Secret beim
--                      Forwarding, at-rest AES-GCM-verschluesselt (AccessTokenConverter);
--                      identisch zu app.whatsapp.gateway.inbound-secret im Backend.

ALTER TABLE studio_config ADD COLUMN IF NOT EXISTS backend_base_url VARCHAR(255);
ALTER TABLE studio_config ADD COLUMN IF NOT EXISTS forward_secret   TEXT;
