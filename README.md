# 📄 OCR Platform

> Plateforme de gestion documentaire avec OCR automatique et recherche full-text.
> Architecture microservices événementielle (event-driven), production-ready.

[![document-service CI](https://github.com/seckkane/ocr-platform/actions/workflows/document-service-ci.yml/badge.svg?branch=develop)](https://github.com/seckkane/ocr-platform/actions/workflows/document-service-ci.yml)
[![CodeQL](https://github.com/seckkane/ocr-platform/actions/workflows/codeql.yml/badge.svg?branch=develop)](https://github.com/seckkane/ocr-platform/actions/workflows/codeql.yml)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.14-green.svg)](https://spring.io/projects/spring-boot)
[![Conventional Commits](https://img.shields.io/badge/Conventional%20Commits-1.0.0-yellow.svg)](https://conventionalcommits.org)
[![License](https://img.shields.io/badge/license-Internal-blue.svg)](LICENSE)

---

## 🎯 Vue d'ensemble

OCR Platform est un système distribué permettant à un utilisateur de :
- **Uploader** des documents (PDF, images)
- **Extraire automatiquement** le texte via OCR (PDF natifs ou scannés)
- **Rechercher** dans le contenu des documents (full-text)

L'architecture suit les principes **microservices** et **event-driven**, avec une communication asynchrone via Apache Kafka.

---

## ✅ Statut actuel

| Service | Stack | Statut |
|---------|-------|--------|
| `document-service` | Java 21 / Spring Boot 3.5 | ✅ **Production-ready + Staging-ready** |
| `ocr-service` | Python 3.12 / FastAPI / Tesseract | 🚧 À démarrer (Phase 8) |
| `search-service` | Java 21 / Spring Boot / Elasticsearch | 📋 Planifié |
| `gateway` | Spring Cloud Gateway | 📋 Planifié |

---

## 🏗️ Architecture

### Vue d'ensemble

```
                              +-------------+
                              |   CLIENT    |
                              +------+------+
                                     | HTTPS + JWT
                                     v
                              +-------------+
                              |   GATEWAY   |  Spring Cloud Gateway
                              | Auth/Routing|  Rate-limit . Routing
                              +------+------+
                  +------------------+------------------+
                  v                  v                  v
          +---------------+  +---------------+  +---------------+
          | document-svc  |  |  ocr-service  |  | search-service|
          |  Spring Boot  |  |    FastAPI    |  |  Spring Boot  |
          | Upload+MinIO  |  | Tesseract OCR |  | Elasticsearch |
          +-------+-------+  +-------+-------+  +-------^-------+
                  |                  |                  |
                  +------------------v------------------+
                                  Kafka
                            (event broker)

  +----------------------------------------------------------------+
  |                       INFRASTRUCTURE                           |
  |   MinIO  .  MySQL  .  PostgreSQL  .  Elasticsearch  .  Redis   |
  |              Keycloak  .  Prometheus  .  Grafana               |
  +----------------------------------------------------------------+
```

### Flow de traitement d'un document

1. Le client uploade un PDF via la **Gateway** (authentifié par JWT Keycloak).
2. Le **document-service** stocke le fichier dans **MinIO**, les métadonnées dans **MySQL**, puis publie un event `documents.uploaded` dans **Kafka**.
3. L'**ocr-service** consomme l'event, télécharge le fichier, extrait le texte (OCR si nécessaire) et publie `documents.ocr.completed`.
4. Le **search-service** consomme cet event et indexe le contenu dans **Elasticsearch**.
5. Le client peut alors rechercher dans le contenu via la **Gateway** → **search-service**.

### Communication inter-services

| Communication | Type | Protocole |
|---|---|---|
| Client → Gateway → Services | Synchrone | HTTP/REST + JWT |
| Service → Service | **Asynchrone** | **Apache Kafka** (events) |
| Service → BDD | Synchrone | JDBC (Java) / asyncpg (Python) |
| Service → Storage | Synchrone | MinIO S3-compatible API |

> 💡 Le pattern **event-driven via Kafka** garantit le découplage : un service peut tomber en panne sans bloquer les autres. Les events sont rejouables (replay) en cas de besoin.

---

## 📁 Structure du monorepo

```
ocr-platform/
├── .github/
│   ├── workflows/                # GitHub Actions (CI, CodeQL, deploy-staging)
│   └── dependabot.yml            # Updates auto des dépendances
├── services/                     # Microservices applicatifs
│   ├── document-service/         # Spring Boot — Upload & stockage
│   ├── ocr-service/              # FastAPI — Extraction OCR (à venir)
│   ├── search-service/           # Spring Boot — Indexation (à venir)
│   └── gateway/                  # Spring Cloud Gateway (à venir)
├── infrastructure/               # Configurations infra (à venir)
├── docs/                         # Documentation projet
├── scripts/                      # Scripts utilitaires
├── .env.staging.example          # Template variables staging
├── .env.prod.example             # Template variables prod
├── .editorconfig                 # Formatage uniforme inter-IDE
├── .gitleaks.toml                # Config détection secrets
├── .pre-commit-config.yaml       # Hooks Git
├── docker-compose.infra.yml      # Stack infra dev local
├── docker-compose.staging.yml    # Stack complète VPS staging
└── README.md
```

---

## 🛠️ Stack technique

### Backend

| Domaine | Technologie | Version |
|---------|-------------|---------|
| Langage backend | Java | 21 (LTS) |
| Langage OCR | Python | 3.12 |
| Framework Java | Spring Boot | 3.5.14 |
| Framework Python | FastAPI | 0.115+ |
| Build Java | Maven (wrapper) | 3.9+ |

### Persistance & Messaging

| Domaine | Technologie | Version |
|---------|-------------|---------|
| Base de données | MySQL | 8.4 |
| Stockage objet | MinIO | latest (S3-compatible) |
| Recherche | Elasticsearch | 8.x |
| Messaging | Apache Kafka (KRaft) | 3.8 |
| Cache | Redis | 7.x |

### Sécurité & Observabilité

| Domaine | Technologie |
|---------|-------------|
| Auth / SSO | Keycloak 26 (OIDC + JWT) |
| Métriques | Micrometer + Prometheus |
| Logs corrélés | Logback + MDC (correlationId) |
| Dashboards | Grafana |

### Qualité & DevSecOps

| Outil | Rôle |
|-------|------|
| **JaCoCo** | Couverture de tests |
| **SpotBugs** | Détection de bugs Java |
| **Checkstyle** | Style de code (Google Java Style) |
| **OWASP Dependency-Check** | Scan CVE des dépendances |
| **CodeQL** | SAST (Static Application Security Testing) |
| **Trivy** | Scan filesystem + futur scan Docker images |
| **Hadolint** | Linter Dockerfile |
| **Gitleaks** | Détection de secrets dans le code |
| **Dependabot** | Updates auto des dépendances |
| **Pre-commit hooks** | Vérifications avant commit en local |

---

## 🚀 Démarrage rapide

### Prérequis

- Java 21+ (Temurin recommandé)
- Docker Desktop 24+
- Git
- Python 3.12+ (pour ocr-service, à venir)
- `uv` (pour pre-commit hooks)

### 1. Cloner le repo

```bash
git clone https://github.com/seckkane/ocr-platform.git
cd ocr-platform
```

### 2. Démarrer l'infrastructure (MySQL, MinIO, Kafka, Keycloak)

```bash
docker compose -f docker-compose.infra.yml up -d
docker compose -f docker-compose.infra.yml ps
```

### 3. Démarrer document-service

```bash
cd services/document-service
./mvnw spring-boot:run
```

L'API est disponible sur http://localhost:8081 :
- **Swagger UI** : http://localhost:8081/swagger-ui.html
- **Health** : http://localhost:8081/actuator/health
- **Métriques Prometheus** : http://localhost:8081/actuator/prometheus

> 📖 Pour le détail spécifique au service, voir [`services/document-service/README.md`](./services/document-service/README.md).

### 4. Setup pre-commit hooks (pour contribuer)

```bash
uv tool install pre-commit
pre-commit install
pre-commit install --hook-type commit-msg
```

---

## 🌍 Stratégie multi-environnement

Le projet utilise **3 environnements** + un profil de test, alignés sur le Git Flow.

### Profils Spring Boot

| Profile | Quand activé | Usage |
|---------|--------------|-------|
| `dev` | Défaut local (aucune env var) | Dev quotidien sur ta machine, infra Docker locale |
| `test` | Auto via `@ActiveProfiles("test")` | Tests d'intégration Testcontainers |
| `staging` | `SPRING_PROFILES_ACTIVE=staging` | Pré-production sur VPS (UAT, démos, validation) |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | Production réelle |

Les fichiers de config sont dans `services/document-service/src/main/resources/` :

```
application.yml           # Commun (port, JPA, actuator, springdoc)
application-dev.yml       # Credentials Docker locaux hardcodés (OK car local)
application-test.yml      # URLs injectées par Testcontainers
application-staging.yml   # 100% env-vars (fail-fast si variable manquante)
application-prod.yml      # 100% env-vars + hardening maximal
```

### Niveaux de durcissement

| Aspect | dev | staging | prod |
|--------|-----|---------|------|
| Secrets | Hardcodés (Docker local) | Env vars obligatoires | Env vars obligatoires |
| Logs | DEBUG/INFO verbeux | INFO | WARN + INFO app uniquement |
| `format_sql` | true (lisible) | false | false |
| Actuator endpoints | health, info, metrics, prometheus | idem (auth requise) | health, info, prometheus |
| `health.show-details` | always | when-authorized | never |
| Flyway `baseline-on-migrate` | true | false | false |
| Stacktraces HTTP | défaut Spring | masquées | masquées + whitelabel off |

### Mapping Git Flow → Environnement

| Branche | Environnement cible | Trigger deploy |
|---------|---------------------|----------------|
| `feat/*` | dev local + CI tests | Pre-commit hooks + GitHub Actions |
| `develop` | staging (futur) | Sera : push merge → deploy staging auto |
| `main` | prod (futur) | Sera : tag `v*.*.*` → deploy prod manuel |

### Lancer document-service en local (profile dev)

```bash
# Aucune variable d'env requise, dev est le défaut
cd services/document-service
./mvnw spring-boot:run
```

### Lancer en staging ou prod (futur, sur VPS)

```bash
# 1. Copier le template approprié
cp .env.staging.example .env.staging

# 2. Remplir avec les VRAIES valeurs
nano .env.staging

# 3. Lancer via docker-compose en chargeant le fichier
docker compose --env-file .env.staging -f docker-compose.staging.yml up -d
```

> ⚠️ **Les fichiers `.env.staging` et `.env.prod` ne sont JAMAIS commités** (ils sont dans `.gitignore`).
> Seuls les templates `.env.staging.example` et `.env.prod.example` sont versionnés.

---

## 🧪 Tests & Qualité

document-service utilise des **profils Maven** pour adapter la rigueur au contexte :

### Run rapide (quotidien)

```bash
cd services/document-service
./mvnw verify
```

→ Tests unitaires + intégration Testcontainers + JaCoCo (~3 min)

### Run qualité (avant push)

```bash
./mvnw verify -P quality
```

→ + SpotBugs + Checkstyle (~5 min)

### Run sécurité (nightly)

```bash
./mvnw verify -P security
```

→ + OWASP Dependency-Check (~10 min, cache NVD 24h)

### Run complet

```bash
./mvnw verify -P all
```

### Rapports générés

| Rapport | Chemin |
|---------|--------|
| Couverture JaCoCo | `target/site/jacoco/index.html` |
| OWASP CVE | `target/dependency-check-report.html` |
| SpotBugs | `target/spotbugsXml.xml` |
| Checkstyle | `target/checkstyle-result.xml` |

---

## 📊 Pipeline CI/CD

| Job | Trigger | Durée |
|-----|---------|-------|
| `secrets-scan` (gitleaks) | push/PR | ~1 min |
| `test` (Maven verify) | push/PR | ~5 min |
| `quality` (SpotBugs + Checkstyle) | PR uniquement | ~5 min |
| `security` (OWASP) | cron nightly + manual | ~10 min |
| `trivy-scan` (filesystem CVE) | push/PR | ~2 min |
| `CodeQL` (SAST) | push/PR + cron weekly | ~10 min |
| `deploy-staging` (build + push GHCR) | push develop + manual | ~5 min |

Caches optimisés : Maven repository, NVD database (~300 MB), GHA Docker layer cache.

---

## 🔄 Workflow Git

On suit un **Git Flow simplifié** :

- **`main`** : production (protégée, merge via PR uniquement)
- **`develop`** : intégration (protégée, merge via PR uniquement)
- **`feat/<nom>`** : feature branches → mergent dans `develop`
- **`hotfix/<nom>`** : bug critique en prod → mergent dans `main` + `develop`
- **`docs/<nom>`** : documentation pure → mergent dans `develop`

### Conventions de commit

[Conventional Commits](https://conventionalcommits.org) **obligatoire** (validé par hook `commit-msg`) :

| Préfixe | Usage |
|---------|-------|
| `feat:` | Nouvelle fonctionnalité |
| `fix:` | Correction de bug |
| `chore:` | Maintenance (deps, config) |
| `docs:` | Documentation |
| `refactor:` | Refactoring sans changement fonctionnel |
| `test:` | Tests |
| `ci:` | CI/CD |
| `style:` | Formatage |
| `perf:` | Optimisation perf |

---

## 📚 Documentation

- **Service document-service** : [`services/document-service/README.md`](./services/document-service/README.md)
- **Bilan technique exhaustif** : [`DOCUMENT-SERVICE-COMPLETED.md`](./DOCUMENT-SERVICE-COMPLETED.md)
- **Architecture détaillée** : [`docs/architecture/`](./docs/architecture/)
- **Décisions d'architecture (ADR)** : [`docs/adr/`](./docs/adr/)
- **API specs** : [`docs/api/`](./docs/api/)

---

## 👤 Auteur

**Issa Seck Kane**
- 📧 issaseckkane@gmail.com
- 💼 [LinkedIn](https://www.linkedin.com/in/issaseckkane)
- 🐙 [GitHub](https://github.com/seckkane)

Projet réalisé dans le cadre d'une montée en compétence sur les architectures microservices, event-driven et la culture DevSecOps.

---

## 📄 Licence

Projet portfolio en cours de développement. Internal use.
