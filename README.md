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
| `document-service` | Java 21 / Spring Boot 3.5 | ✅ **Production-ready** |
| `ocr-service` | Python 3.12 / FastAPI / Tesseract | 🚧 En cours |
| `search-service` | Java 21 / Spring Boot / Elasticsearch | 📋 Planifié |
| `gateway` | Spring Cloud Gateway | 📋 Planifié |

---

## 🏗️ Architecture

## 🏗️ Architecture

```text
                    ┌─────────────┐
                    |   CLIENT         |
                    └──────┬──────┘
                            HTTPS
                    ┌──────▼──────┐
                    |   GATEWAY        │
                    | Spring Boot      │
                    | Auth · Routing · Rate Limit
                    └──┬────┬────┬──┘
        ┌───────────┘            └──────────────┐
        │                      |                         |
┌───────▼────────┐ ┌────────▼────────┐ ┌────────▼────────┐
│ document-svc         | │  ocr-service          | │  search-svc           │
│ Spring Boot          | │   FastAPI             | │ Spring Boot           │
│ Upload + MinIO       | │ PaddleOCR/Tess        | │ Elasticsearch         │
└───────┬────────┘ └────────┬────────┘ └────────▲────────┘
           │                         │                         │
           └───────────► Kafka  ◄───────────────────┘

┌────────────────────────────────────────────────────────────┐
│                    INFRASTRUCTURE                                              │
│ MinIO · Kafka · Elasticsearch · MySQL · Redis                                  │
│ Keycloak · Prometheus · Grafana                                                │
└────────────────────────────────────────────────────────────┘


### Flow de traitement d'un document

1. Le client uploade un PDF via la **Gateway** (authentifié par JWT Keycloak).
2. Le **document-service** stocke le fichier dans **MinIO**, les métadonnées dans **MySQL**, et publie un event `documents.uploaded` dans **Kafka**.
3. L'**ocr-service** consomme l'event, télécharge le fichier, extrait le texte (OCR si nécessaire) et publie `documents.ocr.completed`.
4. Le **search-service** consomme cet event et indexe le contenu dans **Elasticsearch**.
5. Le client peut alors rechercher dans le contenu via la **Gateway** → **search-service**.

---

## 📁 Structure du monorepo

ocr-platform/
├── .github/
│   ├── workflows/             # GitHub Actions (CI, CodeQL)
│   └── dependabot.yml         # Updates auto des dépendances
├── services/                  # Microservices applicatifs
│   ├── document-service/      # Spring Boot — Upload & stockage
│   ├── ocr-service/           # FastAPI — Extraction OCR (à venir)
│   ├── search-service/        # Spring Boot — Indexation (à venir)
│   └── gateway/               # Spring Cloud Gateway (à venir)
├── infrastructure/            # Configurations infra
│   ├── keycloak/              # Realm exports
│   ├── kafka/                 # Topics initialization
│   ├── minio/                 # Buckets initialization
│   └── monitoring/            # Prometheus + Grafana configs
├── docs/                      # Documentation projet
│   ├── architecture/          # Schémas et descriptions
│   ├── adr/                   # Architecture Decision Records
│   └── api/                   # Spécifications OpenAPI
├── scripts/                   # Scripts utilitaires
├── .editorconfig              # Formatage uniforme inter-IDE
├── .gitleaks.toml             # Config détection secrets
├── .pre-commit-config.yaml    # Hooks Git
├── docker-compose.infra.yml   # Stack infra dev local
└── README.md

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

### 4. Setup pre-commit hooks (pour contribuer)

```bash
uv tool install pre-commit
pre-commit install
pre-commit install --hook-type commit-msg
```

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

Caches optimisés : Maven repository, NVD database (~300 MB).

---

## 🔄 Workflow Git

On suit un **Git Flow simplifié** :

- **`main`** : production (protégée, merge via PR uniquement)
- **`develop`** : intégration (protégée, merge via PR uniquement)
- **`feat/<nom>`** : feature branches → mergent dans `develop`
- **`hotfix/<nom>`** : bug critique en prod → mergent dans `main` + `develop`

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
