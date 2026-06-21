# whatsapp-gateway

Multi-Tenant-Backend, das als **technischer Betreiber (Tech Provider, Meta-Option 2)** für
mehrere Studios WhatsApp-Nachrichten über die **WhatsApp Cloud API (Meta Graph API)** empfängt
und sendet. Jedes Studio besitzt eine eigene WABA, die im Meta Business Manager manuell als
Partner geteilt wird. Es gibt **kein Frontend** — Studio-Daten werden über admin-geschützte
REST-Endpoints in der DB gepflegt.

Eine **zentrale** Webhook-URL bedient alle Studios; das Routing erfolgt anhand der
`phone_number_id` aus dem Event.

## Tech-Stack

- Java 21, Spring Boot 4, Maven
- PostgreSQL + Flyway
- Spring Data JPA / Hibernate, MapStruct, Lombok
- Spring `RestClient` für ausgehende Graph-API-Aufrufe
- Docker (Multi-Stage), Docker Compose, Caddy (automatisches HTTPS)

## Projektstruktur

```
src/main/java/de/nailsbeauty/whatsappgateway/
├── config/      WhatsAppProperties, AppProperties, AsyncConfig, RestClientConfig,
│                AdminApiKeyFilter, SecurityConfig
├── web/         WebhookController, StudioConfigController, MessageController,
│                GlobalExceptionHandler
├── dto/         request/ + response/ (strikt getrennt, Records)
├── mapper/      StudioConfigMapper (MapStruct)
├── domain/      StudioConfig, InboundEvent, EventType, AccessTokenConverter
├── repository/  StudioConfigRepository, InboundEventRepository
├── service/     StudioConfigService, MessagingService, InboundEventService,
│                WebhookProcessingService
└── whatsapp/    SignatureVerifier, WhatsAppClient
```

## Konfiguration

Alle Parameter sind über Umgebungsvariablen externalisiert. Lokal `.env.example` nach `.env`
kopieren und ausfüllen (`.env` ist in `.gitignore`).

| Variable                      | Beschreibung                                        |
|-------------------------------|-----------------------------------------------------|
| `SERVER_PORT`                 | HTTP-Port der App (Default 8080)                    |
| `WHATSAPP_VERIFY_TOKEN`       | Webhook-Verify-Token (== Meta App Dashboard)        |
| `WHATSAPP_APP_SECRET`         | App-Secret für HMAC-Signaturprüfung                 |
| `WHATSAPP_GRAPH_API_VERSION`  | z. B. `v21.0`                                        |
| `WHATSAPP_GRAPH_API_BASE_URL` | z. B. `https://graph.facebook.com`                  |
| `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` | PostgreSQL-Verbindung                        |
| `ADMIN_API_KEY`               | Schutz der `/api/**`-Endpoints (Header `X-Admin-Api-Key`) |
| `APP_ENCRYPTION_KEY`          | Base64 (32 Byte) AES-256-Schlüssel für `accessToken` |
| `CADDY_NETWORK`               | Docker-Netzwerk des bestehenden Caddy-Containers     |
| `DB_NAME`                     | Name der PostgreSQL-DB (Default `whatsappgateway`)  |

Schlüssel erzeugen:

```bash
openssl rand -base64 32   # APP_ENCRYPTION_KEY
```

## Lokales Setup (ohne Docker)

Voraussetzung: laufende PostgreSQL-Instanz und gesetzte Env-Variablen.

```bash
./mvnw clean package
./mvnw spring-boot:run
./mvnw test
```

## Start mit Docker Compose

```bash
cp .env.example .env      # Werte ausfüllen (insb. Secrets, CADDY_NETWORK)
docker compose up --build
```

Services:
- **app** — Spring-Boot-Anwendung (liest Env aus `.env`); hängt zusätzlich im
  Docker-Netzwerk des bestehenden Caddy (`CADDY_NETWORK`)
- **db** — PostgreSQL mit persistentem Volume (bleibt privat, nicht im Caddy-Netzwerk)

Healthcheck: `GET /actuator/health`.

## Reverse Proxy (bestehender Caddy-Container)

Auf dem Prod-Server läuft bereits ein **Caddy als Container** für mehrere Anwendungen —
dieser Stack bringt daher **keinen eigenen Caddy** mit. Statt eines Host-Ports wird der
`app`-Container in das bestehende Caddy-Netzwerk eingehängt; Caddy übernimmt Domain und HTTPS.

1. Netzwerk-Namen des Caddy-Containers ermitteln und in `.env` als `CADDY_NETWORK` setzen:

   ```bash
   docker network ls
   # oder gezielt am Caddy-Container:
   docker inspect -f '{{json .NetworkSettings.Networks}}' <caddy-container>
   ```

2. Den Site-Block aus `Caddyfile.snippet` in die bestehende Caddy-Konfiguration aufnehmen
   (Domain anpassen) und Caddy neu laden:

   ```caddy
   whatsapp.example.com {
       reverse_proxy whatsapp-gateway-app:8080
   }
   ```

   ```bash
   docker exec <caddy-container> caddy reload --config /etc/caddy/Caddyfile
   ```

**Wichtig:** Der Proxy muss den **Raw-Body unverändert** durchreichen (Caddy tut das
standardmäßig) — Voraussetzung für die HMAC-Signaturprüfung. Die Webhook-URL im Meta
Dashboard ist dann `https://<deine-domain>/webhook`.

## REST-API

Öffentlich:
- `GET  /webhook` — Verifizierung (gibt `hub.challenge` zurück)
- `POST /webhook` — Events empfangen (HMAC-signiert)
- `GET  /actuator/health` — Healthcheck

Admin (Header `X-Admin-Api-Key: <ADMIN_API_KEY>`):
- `GET    /api/studios`
- `GET    /api/studios/{studioId}`
- `POST   /api/studios`
- `PUT    /api/studios/{studioId}`
- `DELETE /api/studios/{studioId}`
- `POST   /api/studios/{studioId}/messages` — `{ "to": "...", "text": "..." }`
- `POST   /api/studios/{studioId}/messages/template` — `{ "to": "...", "templateName": "...", "languageCode": "de" }`
- `POST   /api/studios/{studioId}/subscribe` — App auf WABA abonnieren

Beispiel: Studio anlegen

```bash
curl -X POST http://localhost:8080/api/studios \
  -H "X-Admin-Api-Key: $ADMIN_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
        "studioId": "studio-berlin",
        "wabaId": "1234567890",
        "phoneNumberId": "9876543210",
        "displayPhoneNumber": "+49 30 1234567",
        "accessToken": "EAAB...",
        "active": true
      }'
```

## Einrichtung im Meta App Dashboard

1. **Webhook-URL** eintragen: `https://<deine-domain>/webhook` (die im bestehenden Caddy
   konfigurierte Domain).
2. **Verify-Token** = Wert von `WHATSAPP_VERIFY_TOKEN`. Meta ruft `GET /webhook` auf und erwartet
   den `hub.challenge` als Klartext zurück.
3. Webhook-Felder abonnieren: mindestens `messages`.
4. **App-Secret** (App → Einstellungen → Allgemein) als `WHATSAPP_APP_SECRET` hinterlegen — damit
   wird die `X-Hub-Signature-256` der eingehenden POSTs geprüft.

## Manuelles WABA-Partner-Sharing & `subscribed_apps`

- Das Partner-Sharing der WABA erfolgt **manuell** im Meta Business Manager (Business-ID des
  Tech Providers als Partner hinzufügen). Das Backend hält nur die resultierenden IDs/Tokens.
- Nach dem Sharing muss die App **einmalig** auf die WABA abonniert werden:
  `POST https://graph.facebook.com/<version>/<waba-id>/subscribed_apps`. Dafür gibt es den
  Komfort-Endpoint `POST /api/studios/{studioId}/subscribe`.

## Sicherheit & Idempotenz

- **Signaturprüfung**: HMAC-SHA256 über den Raw-Body gegen `X-Hub-Signature-256` (zeitkonstanter
  Vergleich). Ungültig → `401`.
- **Asynchrone Verarbeitung**: Der Webhook antwortet sofort mit `200`; die Verarbeitung läuft im
  `webhookExecutor`, damit Meta keine Retries (Duplikate) auslöst.
- **Idempotenz**: Jede `messages[].id` / `statuses[].id` wird über `InboundEvent.metaId` (unique)
  dedupliziert.
- **Secrets**: `accessToken` wird at-rest AES-256-GCM-verschlüsselt gespeichert, nie geloggt und
  nie in Responses zurückgegeben.
