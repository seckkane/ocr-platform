# 📄 document-service

> Microservice de gestion documentaire de la plateforme **OCR Platform**.
> Upload, stockage et métadonnées des documents (PDF, images).

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-green.svg)](https://spring.io/projects/spring-boot)
[![Coverage](https://img.shields.io/badge/coverage-50%25-yellow.svg)](./target/site/jacoco/index.html)

---

## 🎯 Responsabilités

- ✅ Upload de fichiers (PDF, images) via API REST authentifiée JWT
- ✅ Stockage binaire dans **MinIO** (S3-compatible) avec clés calculées (`yyyy/MM/dd/<uuid>.<ext>`)
- ✅ Persistance des métadonnées (id, owner, taille, statut) en **MySQL**
- ✅ Publication de events `documents.uploaded` dans **Kafka** après commit BDD
- ✅ Audit trail event-driven (table `audit_log` avec correlationId)
- ✅ Exposition d'une API REST + Swagger UI

## ❌ Hors-périmètre

- Extraction OCR du contenu → **ocr-service** (à venir)
- Indexation full-text → **search-service** (à venir)
- Auth centralisée → **gateway** (à venir)

---

## 🚀 Démarrage rapide

### Prérequis

- Java 21 (Temurin recommandé)
- Docker Desktop pour l'infra (MySQL, MinIO, Kafka, Keycloak)
- Maven Wrapper inclus (`./mvnw` Linux/Mac, `.\mvnw.cmd` Windows)

### 1. Lancer l'infrastructure

Depuis la racine du repo :

```bash
docker compose -f docker-compose.infra.yml up -d
docker compose -f docker-compose.infra.yml ps
```

### 2. Lancer le service en mode dev

```bash
cd services/document-service
./mvnw spring-boot:run
```

L'API démarre sur **http://localhost:8081** avec le profile `dev` actif.

### 3. Endpoints utiles

| Endpoint | Description |
|---|---|
| http://localhost:8081/swagger-ui.html | Documentation interactive de l'API |
| http://localhost:8081/v3/api-docs | OpenAPI spec JSON |
| http://localhost:8081/actuator/health | Health check |
| http://localhost:8081/actuator/prometheus | Métriques Prometheus |

---

## 🧪 Tests

### Tests rapides (Testcontainers)

```bash
./mvnw verify
```

Lance MySQL + MinIO + Kafka en containers et exécute les tests d'intégration. ~3-5 min selon machine.

### Avec qualité (SpotBugs + Checkstyle)

```bash
./mvnw verify -P quality
```

### Avec sécurité (OWASP CVE scan)

```bash
./mvnw verify -P security
```

### Tout

```bash
./mvnw verify -P all
```

---

## 🐳 Build Docker

### Build local

Depuis le dossier `services/document-service/` :

```bash
docker build -t document-service:local .
```

Image finale : ~398 MB (multi-stage : JDK build → JRE runtime).

### Image officielle (GHCR)

À chaque push sur `develop`, une image est buildée et publiée :

```bash
docker pull ghcr.io/seckkane/ocr-platform/document-service:develop
```

Tags disponibles :
- `develop` (toujours = derniers commits develop)
- `develop-<sha>` (immutable, pour rollback)

---

## 🌍 Profils Spring Boot

| Profile | Quand activé | Usage |
|---------|--------------|-------|
| `dev` | défaut local | Dev quotidien, infra Docker locale |
| `test` | `@ActiveProfiles("test")` | Tests Testcontainers |
| `staging` | `SPRING_PROFILES_ACTIVE=staging` | VPS pré-prod |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | VPS production |

Voir le [README racine](../../README.md#-stratégie-multi-environnement) pour la matrice de durcissement complète.

---

## 📂 Architecture du code

```
src/main/java/com/ocrplatform/document/
├── DocumentServiceApplication.java
├── aspect/                  # AOP (LoggingAspect)
├── audit/                   # Audit trail event-driven
│   └── entity/, event/, listener/, mapper/, service/, repository/
├── config/                  # Beans Spring (Async, Kafka, MinIO, Security, Metrics)
├── controller/              # API REST (DocumentController, AdminAuditController)
├── exception/               # Hierarchie d'exceptions + GlobalExceptionHandler (RFC 7807)
├── filter/                  # CorrelationIdFilter (MDC + X-Correlation-Id)
├── health/                  # MinioHealthIndicator
├── messaging/               # Producer Kafka + listeners @TransactionalEventListener
├── model/                   # Entities + DTOs request/response
├── repository/              # Spring Data JPA
├── security/                # AuthenticatedUser
└── service/                 # Logique métier (Document, Storage, Audit)
```

### Décisions architecturales clés

1. **Event-driven async** : les uploads publient des `DocumentUploadedEvent` Spring (pas Kafka direct). Les listeners (audit + Kafka publisher) consomment indépendamment.
2. **`@TransactionalEventListener(AFTER_COMMIT)`** sur le publisher Kafka : pas de message Kafka si la transaction BDD a rollback.
3. **`REQUIRES_NEW` sur audit** : l'audit est best-effort, ne fait jamais échouer un upload.
4. **MDC + correlationId** : chaque request a son ID, propagé dans tous les logs (sync + async via `MdcTaskDecorator`).
5. **AOP LoggingAspect** : intercepte tous les services, log WARN si méthode > 500ms.

---

## 🗄️ Base de données

Migrations Flyway dans `src/main/resources/db/migration/` :

- `V1__create_documents_table.sql` : table `documents` (UUID, owner, status, storage_key, size_bytes, ...)
- `V2__create_audit_log_table.sql` : table `audit_log` (event_type, severity, correlation_id, JSON metadata)

Hibernate `ddl-auto: validate` : Hibernate vérifie juste que le schéma BDD correspond aux entités, **jamais ne le génère**.

---

## 🔐 Sécurité

- JWT Bearer token via `oauth2ResourceServer` Spring Security
- Decoder : Keycloak JWKS (en prod) ou `MockJwtDecoder` en test
- `@PreAuthorize` sur controllers (`ensureOwnerOrAdmin`, `hasRole('ADMIN')`)
- TestControllers (`TestExceptionController`, `TestStorageController`, `TestAuditController`) en `@Profile("dev")` — non chargés en staging/prod

---

## 📊 Observabilité

### Métriques Prometheus

- `documents.uploaded` (counter)
- `documents.failed` (counter)
- `document.upload.duration` (timer p50/p95/p99)
- `document.size` (distribution histogram)

### Health checks

- `/actuator/health` : status général
- `/actuator/health/minio` : custom MinioHealthIndicator
- `/actuator/health/db`, `/actuator/health/kafka` : auto Spring Boot

### Logs

- Format structuré via Logback
- `correlationId` dans tous les logs (header `X-Correlation-Id` ou auto-généré)
- Niveau ajustable via `application-{profile}.yml`

---

## 📚 Documentation complémentaire

- [README racine](../../README.md) — Vue plateforme + Git Flow + CI/CD
- [`DOCUMENT-SERVICE-COMPLETED.md`](../../DOCUMENT-SERVICE-COMPLETED.md) — Bilan technique exhaustif (versions, packages, historique commits)

---

## 👤 Auteur

**Issa Seck Kane** — [LinkedIn](https://www.linkedin.com/in/issaseckkane) · [GitHub](https://github.com/seckkane)
