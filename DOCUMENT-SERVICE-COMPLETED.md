# ✅ Document-service — Bilan technique complet

> Document-service est **production-ready** et **staging-ready**. Ce fichier liste exhaustivement ce qui a été accompli, pour ne rien refaire et fournir un contexte technique complet aux futures sessions de mentoring.

---

## 📦 Versions exactes (source of truth)

| Composant | Version | Notes |
|-----------|---------|-------|
| Java | 21 (Temurin) | LTS |
| Spring Boot | 3.5.14 | parent POM |
| Maven | via Wrapper (mvnw) | 3.9+ |
| MySQL | 8.4 | testcontainers `mysql:8.4` |
| MinIO Java SDK | 8.5.17 | |
| Apache Kafka | 3.8.0 | testcontainers `apache/kafka:3.8.0` |
| Keycloak | 26.0 | image `quay.io/keycloak/keycloak:26.0` |
| SpringDoc OpenAPI | 2.7.0 | |
| Testcontainers | 1.21.4 | transitive depuis Spring |
| JaCoCo Maven plugin | 0.8.13 | |
| SpotBugs Maven plugin | 4.9.3.0 | |
| Checkstyle Maven plugin | 3.6.0 | google_checks.xml |
| OWASP DC Maven plugin | 11.1.1 | |
| pre-commit | 4.6.0 | via uv |
| gitleaks | v8.18.4 | downgrade depuis v8.22.1 (bug WASM Windows) |
| hadolint | v2.13.1-beta | linter Dockerfile |
| Docker base image (build) | `eclipse-temurin:21-jdk-jammy` | |
| Docker base image (runtime) | `eclipse-temurin:21-jre-jammy` | |

---

## 🏗️ Architecture du code

### Package structure (`com.ocrplatform.document`)

```
document/
├── DocumentServiceApplication.java
├── aspect/
│   └── LoggingAspect.java                  # AOP, logs WARN si > 500ms
├── audit/
│   ├── converter/MapToJsonConverter.java   # JPA AttributeConverter
│   ├── entity/AuditLog.java
│   ├── enums/AuditEventType.java
│   ├── enums/AuditSeverity.java
│   ├── event/BaseAuditEvent.java          # parent event abstract
│   ├── event/DocumentUploadedEvent.java
│   ├── event/DocumentFailedEvent.java
│   ├── listener/AuditEventListener.java   # @Async, REQUIRES_NEW
│   ├── mapper/AuditLogMapper.java
│   ├── repository/AuditLogRepository.java
│   ├── service/AuditService.java          # interface
│   └── service/AuditServiceImpl.java      # @Transactional REQUIRES_NEW best-effort
├── config/
│   ├── AsyncConfig.java                   # auditExecutor pool + MdcTaskDecorator
│   ├── JpaConfig.java
│   ├── KafkaTopicsConfig.java
│   ├── KafkaTopicsProperties.java         # @ConfigurationProperties
│   ├── MetricsConfig.java                 # 4 beans Prometheus
│   ├── MinioConfig.java
│   ├── OpenApiConfig.java
│   ├── SecurityConfig.java                # JWT Keycloak + roles
│   └── StorageProperties.java
├── controller/
│   ├── DocumentController.java            # @PreAuthorize ensureOwnerOrAdmin
│   ├── AdminAuditController.java          # ROLE_ADMIN
│   ├── TestAuditController.java           # @Profile("dev") — debug
│   ├── TestExceptionController.java       # @Profile("dev") — tests exception handling
│   └── TestStorageController.java         # @Profile("dev") — tests upload MinIO direct
├── exception/
│   ├── BaseException.java
│   ├── BusinessRuleException.java
│   ├── DocumentNotFoundException.java
│   ├── ErrorCode.java                     # enum codes DOC-001, DOC-002, etc.
│   ├── GlobalExceptionHandler.java        # RFC 7807 ProblemDetail
│   └── StorageException.java
├── filter/
│   └── CorrelationIdFilter.java           # MDC correlationId via X-Correlation-Id header
├── health/
│   └── MinioHealthIndicator.java          # custom health Actuator
├── messaging/
│   ├── event/DocumentUploadedKafkaEvent.java   # record
│   ├── listener/DocumentKafkaPublisher.java    # @TransactionalEventListener AFTER_COMMIT
│   └── producer/DocumentKafkaProducer.java
├── model/
│   ├── dto/request/DocumentUploadRequest.java
│   ├── dto/response/DocumentResponse.java
│   ├── dto/response/PageResponse.java
│   ├── dto/response/AuditLogResponse.java
│   ├── entity/BaseEntity.java             # @SuperBuilder + @Builder.Default UUID
│   ├── entity/Document.java               # @DynamicUpdate
│   └── enums/DocumentStatus.java
├── repository/
│   └── DocumentRepository.java
├── security/
│   └── AuthenticatedUser.java
└── service/
    ├── DocumentService.java               # interface
    ├── DocumentServiceImpl.java           # validate → MinIO → BDD → events
    ├── StorageService.java                # interface
    └── StorageServiceImpl.java            # MinIO impl, key yyyy/MM/dd/<uuid>.<ext>
```

### Décisions architecturales clés

1. **Event-driven async** — uploads publient des `DocumentUploadedEvent` Spring (pas Kafka direct) → `AuditEventListener` (BDD) + `DocumentKafkaPublisher` (Kafka) consomment indépendamment
2. **`@TransactionalEventListener(AFTER_COMMIT)`** sur le publisher Kafka — garantit pas de message Kafka si la transaction BDD a rollback
3. **`REQUIRES_NEW` sur audit** — l'audit est best-effort, ne doit jamais faire échouer un upload
4. **MDC + correlationId** — chaque request a son ID, propagé dans tous les logs (sync + async via `MdcTaskDecorator`)
5. **AOP LoggingAspect** — intercepte tous les services, log WARN si méthode > 500ms
6. **`@DynamicUpdate`** sur `Document` — JPA met à jour seulement les colonnes modifiées
7. **`@SuperBuilder` + `@Builder.Default`** sur `BaseEntity` — pour que les sous-classes héritent du builder avec UUID auto-généré

---

## 🗄️ Base de données

### Migrations Flyway

- **V1__create_documents_table.sql** : table `documents` + index sur `owner_id`, `status`
- **V2__create_audit_log_table.sql** : table `audit_log` + index sur `event_type`, `created_at`, `entity_id`

### Schéma Document

```sql
documents (
  id BINARY(16) PRIMARY KEY,            -- UUID
  original_name VARCHAR(255) NOT NULL,
  storage_key VARCHAR(500) NOT NULL,    -- yyyy/MM/dd/<uuid>.<ext>
  content_type VARCHAR(100),
  size_bytes BIGINT,
  status ENUM(...),
  owner_id VARCHAR(100) NOT NULL,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  version BIGINT
)
```

### Schéma audit_log

```sql
audit_log (
  id BINARY(16) PRIMARY KEY,
  event_type VARCHAR(50),
  severity VARCHAR(20),
  entity_type VARCHAR(50),
  entity_id VARCHAR(100),
  user_id VARCHAR(100),
  correlation_id VARCHAR(100),
  metadata JSON,                         -- via MapToJsonConverter
  created_at TIMESTAMP
)
```

---

## 🔐 Sécurité

### Keycloak realm

- **Realm name** : `ocr-platform`
- **Client** : `ocr-platform-app`
- **User dev** : `issaseckkane` (mot de passe : `admin`)
- **Roles** : `USER`, `ADMIN`

### Spring Security config

- JWT Bearer token via `oauth2ResourceServer`
- Decoder : Keycloak JWKS endpoint (en prod) ou `MockJwtDecoder` en test
- `@PreAuthorize` sur controllers :
  - `ensureOwnerOrAdmin` : SpEL custom
  - `hasRole('ADMIN')` pour endpoints admin

### TestControllers — Whitelist sécurisé

`TestExceptionController`, `TestStorageController`, `TestAuditController` annotés `@Profile("dev")` (whitelist) → chargés UNIQUEMENT en dev local. Non exposés en `test`, `staging`, `prod`.

Pattern senior : whitelist > blacklist (`@Profile("!prod")` aurait laissé les endpoints exposés sur le VPS staging).

---

## 📊 Observabilité

### Métriques Prometheus (`MetricsConfig.java`)

```java
@Bean Counter documentsUploadedCounter        // documents.uploaded
@Bean Counter documentsFailedCounter          // documents.failed
@Bean Timer documentUploadDuration            // p50/p95/p99
@Bean DistributionSummary documentSize        // histogram tailles
```

### Health checks

- `/actuator/health` (Spring Boot)
- `/actuator/health/minio` (custom `MinioHealthIndicator`)
- `/actuator/health/db` (auto via Spring)
- `/actuator/health/kafka` (auto via Spring)

### Endpoints Actuator exposés

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

---

## 🧪 Tests

### Test d'intégration (Testcontainers)

- **Classe** : `DocumentControllerIT` (3 tests verts)
- **Conteneurs lancés** :
  - MySQL 8.4
  - MinIO `minio/minio:latest`
  - Kafka `apache/kafka:3.8.0` (KafkaContainer)
- **AbstractIntegrationTest** : classe de base avec `@DynamicPropertySource`
- **TestSecurityConfig** : MockJwtDecoder pour bypass auth en test

### Coverage JaCoCo

- **Global** : 50% instructions
- **Best** : aspect 90%, listener 100%
- **Worst** : controllers 11%, filter 10%, health 0%, model.entity 0%
- **Seuil minimum actuel** : 0.00 (volontaire au démarrage, à monter à 30% → 50% → 70%)

### Postman

- **Collection** : "OCR - Document Service" (27 requests)
- **Environment** : "OCR Platform - Local Dev"
- **Folders** : Auth / Documents / Validation Errors / Admin / Observability
- **Run Collection** : 18 tests verts auto

---

## 🔧 Qualité — Profils Maven

### Architecture pom.xml

```
mvn verify              → tests + JaCoCo (~3 min) — quotidien
mvn verify -P quality   → + SpotBugs + Checkstyle (~5 min) — sur PR
mvn verify -P security  → + OWASP Dependency-Check (~10 min) — nightly
mvn verify -P all       → tout
```

### OWASP config (warn-only)

```xml
<failBuildOnCVSS>11</failBuildOnCVSS>           <!-- jamais bloquant -->
<nvdValidForHours>24</nvdValidForHours>          <!-- cache NVD 24h -->
<ossindexAnalyzerEnabled>false</ossindexAnalyzerEnabled>   <!-- évite token -->
<assemblyAnalyzerEnabled>false</assemblyAnalyzerEnabled>   <!-- pas de .NET -->
<retireJsAnalyzerEnabled>false</retireJsAnalyzerEnabled>   <!-- pas de JS -->
```

### Résultat actuel

- ✅ 0 SpotBugs warnings
- ✅ 0 Checkstyle violations
- ✅ Hadolint compliant (DL3008 ignoré explicitement)

---

## 🚀 CI/CD — GitHub Actions

### Fichier : `.github/workflows/document-service-ci.yml`

5 jobs avec triggers conditionnels :

| Job | Trigger | Durée | Outils |
|-----|---------|-------|--------|
| `secrets-scan` | push/PR | ~1 min | gitleaks |
| `test` | push/PR | ~3-5 min | Maven verify, JaCoCo upload |
| `quality` | PR uniquement | ~5 min | Maven `-P quality`, SpotBugs/Checkstyle reports |
| `security` | cron `0 2 * * *` + workflow_dispatch | ~10 min | Maven `-P security`, NVD cache |
| `trivy-scan` | push/PR | ~2 min | filesystem CVE scan |

### Fichier : `.github/workflows/codeql.yml`

- SAST officiel GitHub
- Triggers : push/PR develop/main + cron `0 8 * * 1`
- Languages : `java-kotlin`
- Suite : `security-and-quality`

### Fichier : `.github/workflows/deploy-staging.yml`

- Trigger : push develop (paths `services/document-service/**`) + manual dispatch
- Build l'image via le Dockerfile et push sur **GHCR**
- Tags publiés :
  - `ghcr.io/seckkane/ocr-platform/document-service:develop`
  - `ghcr.io/seckkane/ocr-platform/document-service:develop-<sha>`
- Auth : `GITHUB_TOKEN` natif (zéro secret)
- Cache GHA pour builds rapides

---

## 🌍 Multi-environnement & déploiement (Phases A + B + Quick-win)

### Profils Spring Boot

| Profile | Activé via | Usage | Hardening |
|---------|-----------|-------|-----------|
| `dev` | défaut local | Dev quotidien, infra Docker locale | Logs verbeux, secrets hardcodés OK |
| `test` | `@ActiveProfiles("test")` | Tests Testcontainers | URLs dynamiques, JWT mock |
| `staging` | `SPRING_PROFILES_ACTIVE=staging` | VPS pré-prod | Env vars obligatoires, logs INFO, actuator auth |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | VPS prod | Env vars + fail-fast, logs WARN, actuator minimal |

### Fichiers de configuration

```
services/document-service/src/main/resources/
├── application.yml           # Commun strict (port, JPA basics, springdoc)
├── application-dev.yml       # Credentials Docker locaux hardcodés
├── application-test.yml      # Testcontainers (URLs dynamiques)
├── application-staging.yml   # 100% env-vars (fail-fast)
└── application-prod.yml      # 100% env-vars + hardening max
```

### Templates `.env` versionnés

À la racine du repo :
- `.env.staging.example` : variables documentées pour staging VPS
- `.env.prod.example` : variables documentées pour prod (avec notes hardening)

`.gitignore` configuré avec `.env.*` + exception `!.env.*.example`.
`.gitleaks.toml` allowlist les fichiers `*.example`.

### Dockerfile production-ready

`services/document-service/Dockerfile` :
- Multi-stage : `eclipse-temurin:21-jdk-jammy` (build) → `21-jre-jammy` (runtime)
- Image finale ~398 MB
- User non-root `spring:spring`
- Healthcheck Docker sur `/actuator/health/liveness`
- JVM container-aware : `MaxRAMPercentage=75 + UseContainerSupport`
- Maven Wrapper utilisé (reproductibilité)
- Hadolint compliant (DL3008 ignoré explicitement avec rationale)
- OCI metadata labels

### docker-compose.staging.yml

Stack complète pour VPS Contabo :
- MySQL + MinIO + Kafka + Keycloak + document-service
- Sécurité : services internes (MySQL, Kafka) **non exposés** publiquement
- Sécurité : services exposés bind sur `127.0.0.1:PORT` (besoin reverse proxy pour publier)
- `depends_on` avec `service_healthy`
- minio-init container : auto-création du bucket staging
- Volumes nommés persistants

### Reste à faire (Phase B.4 — futur, après Phase 8)

- [ ] SSH GitHub Actions ↔ VPS Contabo
- [ ] Auto-pull + `docker compose up` sur VPS après push image
- [ ] Reverse proxy Traefik + HTTPS Let's Encrypt
- [ ] Configuration domaine `staging.ocrplatform.<tld>`
- [ ] Realm Keycloak staging configuré
- [ ] Hardening VPS (UFW, fail2ban, SSH non-root)

---

## 🛡️ DevSecOps complet

### Pre-commit hooks (`.pre-commit-config.yaml`)

```yaml
- pre-commit-hooks v5.0.0:
    - trailing-whitespace
    - end-of-file-fixer
    - check-yaml (--unsafe pour Spring profiles)
    - check-json
    - check-toml
    - check-merge-conflict
    - check-added-large-files (--maxkb=1000)
    - detect-private-key
    - mixed-line-ending (--fix=lf)
- gitleaks v8.18.4
- conventional-pre-commit v3.6.0 (commit-msg hook)
- hadolint v2.13.1-beta (Dockerfile linter)
```

### Setup local

```powershell
uv tool install pre-commit
pre-commit install
pre-commit install --hook-type commit-msg
```

### Dependabot (`.github/dependabot.yml`)

- **maven** dans `/services/document-service` (limit 5 PR, groupes spring/testcontainers/jackson)
- **github-actions** dans `/` (limit 3 PR)
- **docker** dans `/` (limit 3 PR)
- Schedule : weekly Mondays 06:00 Europe/Paris
- Commit prefixes : `chore(deps)`, `chore(ci)`, `chore(docker)`

---

## 📝 Historique Git complet

### Branche `develop` — 6 PR mergées

| # | Branche | Sujet |
|---|---------|-------|
| #1 | `feat/ci-skeleton` | CI/CD skeleton + outils qualité + Maven Wrapper |
| #2 | `feat/quality-tooling-finalization` | Pre-commit + Dependabot + CodeQL + README enrichi |
| #3 | `feat/multi-environment-strategy` | 4 profils Spring + .env templates + README multi-env |
| #4 | `feat/secure-test-endpoints` | TestControllers `@Profile("dev")` (whitelist) |
| #5 | `feat/staging-deployment` | Dockerfile + docker-compose.staging.yml + workflow GHCR |
| #6 | `docs/update-completion-status` | Mise à jour docs après Phases A + B |

---

## 🎬 Endpoints API exposés

### Public

- `GET /actuator/health` → status général
- `GET /actuator/health/liveness` → liveness probe K8s
- `GET /actuator/health/readiness` → readiness probe K8s
- `GET /actuator/info` → version, build info
- `GET /actuator/prometheus` → métriques Prometheus
- `GET /swagger-ui.html` → documentation interactive
- `GET /v3/api-docs` → OpenAPI spec JSON

### Authentifiés (USER role)

- `POST /api/documents` → upload (multipart/form-data)
- `GET /api/documents/{id}` → détail (owner ou admin)
- `GET /api/documents` → liste paginée (owner)
- `DELETE /api/documents/{id}` → suppression (owner ou admin)

### Admin uniquement (ADMIN role)

- `GET /api/admin/audit-log` → logs d'audit
- `GET /api/admin/audit-log/{entityId}` → logs par entity

### Test/dev (`@Profile("dev")` uniquement)

- `GET /api/test/exception/{type}` → tests exception handling
- `POST /api/test/storage/upload` → upload direct MinIO sans BDD
- `POST /api/test/audit/trigger` → trigger event audit manuel

---

## 🚀 Commandes pour relancer document-service localement

```powershell
# 1. Démarrer infra
cd C:\ProjetML\ocr-platform
docker compose -f docker-compose.infra.yml up -d

# 2. Vérifier infra healthy
docker compose -f docker-compose.infra.yml ps

# 3. Configurer Keycloak realm (si première fois)
# - http://localhost:8180/admin (admin/admin)
# - Import realm: ocr-platform
# - Create user: issaseckkane (password: admin, roles: USER)

# 4. Lancer document-service
cd services/document-service
.\mvnw.cmd spring-boot:run

# 5. Tester
# - http://localhost:8081/swagger-ui.html
# - http://localhost:8081/actuator/health
# - http://localhost:9001 (MinIO Console — minioadmin/minioadmin123)
# - http://localhost:8090 (Kafka UI)
```

---

## ✅ Checklist "production-ready + staging-ready" — Tout coché

- [x] Code organisé en couches (controller/service/repository/etc.)
- [x] Lombok + Spring annotations propres
- [x] Migrations Flyway versionnées
- [x] Sécurité JWT Keycloak fonctionnelle
- [x] Audit trail event-driven async
- [x] Métriques Prometheus exposées
- [x] Health checks custom (MinIO)
- [x] Logs structurés avec correlationId
- [x] Exception handling RFC 7807
- [x] OpenAPI/Swagger documenté
- [x] Tests d'intégration Testcontainers
- [x] JaCoCo coverage
- [x] SpotBugs + Checkstyle (0 violations)
- [x] OWASP CVE scan (warn-only)
- [x] CodeQL SAST
- [x] Trivy filesystem scan
- [x] Gitleaks secrets scan
- [x] Hadolint Dockerfile linter
- [x] Pre-commit hooks
- [x] Conventional Commits enforcés
- [x] Dependabot 3 ecosystems
- [x] CI/CD GitHub Actions multi-jobs
- [x] Branch protection main + develop
- [x] Auto-delete merged branches
- [x] Git Flow respecté
- [x] README enrichi avec badges
- [x] Multi-environnement Spring (dev/test/staging/prod)
- [x] TestControllers whitelist `@Profile("dev")`
- [x] Templates `.env.*.example` versionnés
- [x] Dockerfile multi-stage non-root + healthcheck
- [x] docker-compose.staging.yml prêt VPS
- [x] Workflow deploy-staging → GHCR auto-build
- [x] Image disponible : `ghcr.io/seckkane/ocr-platform/document-service:develop`

---

## 👤 Auteur

**Issa Seck Kane**
- 📧 issaseckkane@gmail.com
- 💼 [LinkedIn](https://www.linkedin.com/in/issaseckkane)
- 🐙 [GitHub](https://github.com/seckkane)
