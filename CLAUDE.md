# CLAUDE.md — whatsapp-gateway

Anleitung für Claude Code beim Arbeiten an **diesem** Sub-Projekt. Dieses Projekt ist
eigenständig und unabhängig vom Nails-&-Beauty-Verwaltungssystem im Repo-Root.

## Projektübersicht

**whatsapp-gateway** — Ein **Multi-Tenant-Backend**, das als **technischer Betreiber
(Tech Provider, Meta-Option 2)** für mehrere Studios WhatsApp-Nachrichten über die
**WhatsApp Cloud API (Meta Graph API)** empfängt und sendet.

- Jedes Studio hat eine **eigene WABA**, die im Meta Business Manager **manuell als
  Partner** mit unserer Business-ID geteilt wird.
- **Kein Frontend.** Studio-Daten (insbesondere `phone_number_id` und `access_token`)
  werden **direkt in der DB** gepflegt — Schreibzugriff nur über admin-geschützte
  REST-Endpoints.
- **Eine** zentrale Webhook-Callback-URL für alle Studios. Das Routing auf das jeweilige
  Studio erfolgt anhand der `phone_number_id` aus dem eingehenden Event.

## Tech-Stack (verbindlich)

- **Java 21**, **Spring Boot 4.x**, Maven. (Java 25 bewusst nicht — Probleme mit
  Spring Boot 4 + Lombok.)
- Schichten: `web` (controller) → `service` → `repository` → `domain` (entity).
- **DTOs strikt getrennt von Entities**, zusätzlich **Request- vs. Response-DTOs**
  (`dto/request`, `dto/response`). Entities werden **nie** nach außen gegeben.
- **Mapping ausschließlich mit MapStruct** (`@Mapper(componentModel = "spring")`) —
  kein `BeanUtils.copyProperties`, kein manuelles Mapping.
- **Lombok** auf Beans/Entities (`@Getter @Setter @NoArgsConstructor`,
  `@RequiredArgsConstructor`, `@Slf4j`) — **niemals auf DTOs** (das sind Records).
- Bean Validation (`jakarta.validation`) **nur auf Request-DTOs**.
- Zentrales Exception-Handling via `@RestControllerAdvice` → einheitliches
  `ErrorResponse`-DTO.
- HTTP-Client für Graph API: Spring **`RestClient`**.
- Datenbank: **PostgreSQL**, Schema-Migrationen ausschließlich über **Flyway**
  (`src/main/resources/db/migration/Vn__*.sql`, fortlaufend, nie ändern).

## Architektur-Kernpunkte (NICHT brechen)

1. **Webhook GET** `/webhook` → Verifizierung: bei korrektem `hub.verify_token` den
   `hub.challenge` als **Klartext** zurückgeben, sonst `403`.
2. **Webhook POST** `/webhook`:
   - **HMAC-SHA256 über den Raw-Body** mit dem App-Secret gegen Header
     `X-Hub-Signature-256` (Format `sha256=<hex>`), **zeitkonstanter Vergleich**.
     Ungültig → `401`. Der Raw-Body wird als `@RequestBody String` gelesen, damit die
     Bytes für HMAC exakt bleiben (Caddy gibt den Body unverändert durch).
   - **Sofort `200`** zurückgeben, Verarbeitung **asynchron** (`@Async`) — sonst retryt
     Meta und erzeugt Duplikate.
   - Routing über `phone_number_id` → `StudioConfig`. Unbekannte Nummer → loggen +
     ignorieren.
   - **Idempotenz**: `messages[].id` / `statuses[].id` über `InboundEvent.metaId`
     (unique) deduplizieren.
3. **Senden**: `POST {baseUrl}/{version}/{phoneNumberId}/messages` mit
   `Authorization: Bearer <accessToken des Studios>`.
4. **Admin-Schutz**: alle `/api/**`-Endpoints hinter Header `X-Admin-Api-Key`
   (`AdminApiKeyFilter`). `/webhook` und `/actuator/health` sind **öffentlich**.
5. **Secrets**: `accessToken` ist **at-rest verschlüsselt** (AES-GCM via
   `AccessTokenConverter`) und wird **nie geloggt** und **nie** in Responses
   zurückgegeben (`StudioConfigResponse` enthält kein Token).

## Robustheit beim JSON-Parsing

Webhook-Payloads werden **null-sicher** über Jackson `JsonNode` (`path(...)`)
geparst — fehlende Felder dürfen nie eine NPE auslösen. Verarbeitung pro Event in
try/catch, damit ein fehlerhaftes Event den Batch nicht abbricht.

## Konfiguration / Env

Alle Parameter sind über Umgebungsvariablen externalisiert; `application.yml`
referenziert ausschließlich `${...}`. Keine Klartext-Secrets im Repo.

| Env-Variable                  | Zweck                                              |
|-------------------------------|----------------------------------------------------|
| `WHATSAPP_VERIFY_TOKEN`       | Webhook-Verify-Token (== Meta App Dashboard)       |
| `WHATSAPP_APP_SECRET`         | App-Secret für HMAC-Signaturprüfung                |
| `WHATSAPP_GRAPH_API_VERSION`  | z. B. `v21.0`                                       |
| `WHATSAPP_GRAPH_API_BASE_URL` | z. B. `https://graph.facebook.com`                 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | PostgreSQL-Verbindung                   |
| `ADMIN_API_KEY`               | Schutz der `/api/**`-Verwaltungs-Endpoints         |
| `APP_ENCRYPTION_KEY`          | Base64 (32 Byte) AES-256-Schlüssel für `accessToken` |
| `SERVER_PORT`                 | HTTP-Port der App im Container                     |
| _(Caddy-Netz)_                | Fest als `proxy` (external) in `docker-compose.yml` — keine Env-Variable |
| `DB_NAME`                     | Name der PostgreSQL-DB (Default `whatsappgateway`) |

Lokal: `.env.example` kopieren → `.env` (nicht eingecheckt). Prod: Docker Compose lädt `.env`.

## Datenmodell

- `StudioConfig`: `id`, `studioId` (unique), `wabaId`, `phoneNumberId` (unique, indexiert),
  `displayPhoneNumber`, `accessToken` (verschlüsselt), `active`, `backendBaseUrl`,
  `forwardSecret` (verschlüsselt), `createdAt`, `updatedAt`.
- `InboundEvent` (Idempotenz/Audit): `id`, `metaId` (unique), `phoneNumberId`, `type`
  (`MESSAGE`/`STATUS`), `payload` (jsonb), `receivedAt`.

## Forwarding eingehender Nachrichten ans Studio-Backend

Pro Studio läuft eine eigene Backend-Instanz. Neu eingegangene **Text-Nachrichten**
(Stempel-Codes) werden vom `WebhookProcessingService` über den `BackendForwarder` an
`POST {backendBaseUrl}/api/public/whatsapp/gateway/inbound` weitergeleitet —
normalisierter JSON-Body (`studioId`, `phoneNumberId`, `from`, `messageId`, `type`,
`text`, `timestamp`), authentifiziert per Header `X-Gateway-Secret` (== `forwardSecret`
des Studios, identisch zu `app.whatsapp.gateway.inbound-secret` im Backend).

Regeln (NICHT brechen):
- Weiterleitung **nur bei neuen** Events (`recordIfNew == true`) → keine Doppel-Stempel
  bei Meta-Redelivery. Status-Updates werden nicht weitergeleitet.
- Nur `type == text` wird weitergeleitet (andere Typen werden verbucht, nicht gepusht).
- **Best-Effort**: Fehler werden geloggt, nicht erneut versucht (Meta retryt nicht mehr,
  da der Webhook bereits mit `200` quittiert wurde). `forwardSecret` nie loggen.
- Fehlen `backendBaseUrl`/`forwardSecret`, unterbleibt die Weiterleitung (nur Audit).

## REST-Endpoints

- `GET  /webhook`                              — Verifizierung (öffentlich)
- `POST /webhook`                              — Events empfangen (öffentlich, signiert)
- `GET  /api/studios`                          — Liste (admin)
- `GET  /api/studios/{studioId}`               — Einzeln (admin)
- `POST /api/studios`                          — Anlegen (admin)
- `PUT  /api/studios/{studioId}`               — Aktualisieren (admin)
- `DELETE /api/studios/{studioId}`             — Löschen (admin)
- `POST /api/studios/{studioId}/messages`          — Textnachricht senden (admin)
- `POST /api/studios/{studioId}/messages/template` — Template senden (admin)
- `POST /api/studios/{studioId}/subscribe`         — App auf WABA abonnieren (admin)
- `GET  /actuator/health`                      — Healthcheck (öffentlich)

## Deployment

- Multi-Stage `Dockerfile` (Build mit JDK 21 + Maven, schlankes JRE-Runtime, non-root).
- `docker-compose.yml`: nur `app` + `db` (Postgres + Volume). **Kein eigener Caddy** —
  auf dem Prod-Server läuft bereits ein **Caddy-Container** für mehrere Anwendungen.
- Der `app`-Container hängt im bestehenden Caddy-Docker-Netzwerk `proxy` (als
  `external` referenziert) **und** im privaten `default`-Netzwerk zur DB. Heißt das
  Caddy-Netz auf dem Server anders, in der `docker-compose.yml` den Namen `proxy`
  anpassen (an den Services und im `networks`-Block). Der bestehende Caddy erreicht die
  App über den Container-Namen `whatsapp-gateway-app:8080`
  — **kein** Host-Port-Mapping. TLS/Let's Encrypt verwaltet der bestehende Caddy.
- `Caddyfile.snippet` enthält den Site-Block (`reverse_proxy whatsapp-gateway-app:8080`)
  zum Einfügen in die bestehende Caddy-Konfiguration.
- **Wichtig:** Der Reverse Proxy muss den **Raw-Body unverändert** durchreichen
  (Pflicht für die HMAC-Signaturprüfung) — keine Body-Manipulation/Komprimierung des
  eingehenden Bodys.

## Build & Run

```bash
mvn clean package
mvn spring-boot:run            # benötigt laufende Postgres-DB + gesetzte Env
docker compose up --build      # vollständiger Stack (.env muss ausgefüllt sein)
mvn test
```

## Konventionen

- Moderne Java-21-Features: Records für DTOs, `var`, Pattern-Matching, Sequenced Collections.
- Constructor-Injection via `@RequiredArgsConstructor`, keine `@Autowired`-Felder.
- `@Transactional` auf Service-Methoden, nicht auf Repos/Controllern.
- Keine Geschäftslogik im Controller.
- Eigene Domain-Exceptions (`NotFoundException`, `ConflictException`,
  `WhatsAppApiException`) — kein nacktes `RuntimeException`.
- Tokens/Secrets niemals loggen.
