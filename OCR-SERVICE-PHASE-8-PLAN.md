# 🚧 OCR-service — Plan détaillé Phase 8

> Ce document est la **roadmap** du 2e microservice à construire. À uploader dans le projet Claude pour que la nouvelle conversation démarre immédiatement avec ce plan en tête.

---

## 🎯 Périmètre fonctionnel

`ocr-service` est un microservice **Python** qui :

1. **Consomme** l'event Kafka `documents.uploaded` du document-service
2. **Télécharge** le fichier depuis MinIO via la `storage_key` reçue
3. **Extrait le texte** via OCR (Tesseract en premier, PaddleOCR plus tard)
4. **Persiste** le résultat en PostgreSQL (avec idempotence)
5. **Publie** un nouvel event Kafka `documents.ocr.completed`
6. **Expose** une API REST pour récupérer les résultats : `GET /api/ocr/{documentId}`

---

## 🎯 Décisions techniques (déjà prises)

| Décision | Choix | Pourquoi |
|----------|-------|----------|
| **Moteur OCR** | Tesseract en premier, **Strategy pattern** | Simple, swap PaddleOCR plus tard (Phase 9) sans changer le code métier |
| **Base de données** | **PostgreSQL** (PAS MongoDB/MySQL) | Préparer pgvector pour recherche sémantique (Phase 10) |
| **Idempotence** | Table `processed_events(event_id, processed_at, status)` | Kafka at-least-once → on doit gérer les replays |
| **Build/Deps** | **uv** + Python 3.12 | Performance, lock file moderne (`uv.lock`) |
| **Web framework** | **FastAPI** | Async natif, OpenAPI auto, type hints |
| **ORM** | **SQLAlchemy 2.0** + **Alembic** | Standard Python, parallèle JPA + Flyway |
| **Tests** | **pytest** + **Testcontainers Python** | Cohérence avec document-service |
| **Logging** | structlog + correlation_id middleware | Cohérent avec MDC Spring |

---

## 🏗️ Architecture cible

```
┌──────────────────────────────────────────────────────────────────┐
│                        ocr-service                               │
│                                                                  │
│  ┌─────────────┐                                                 │
│  │   Kafka     │  ← documents.uploaded                           │
│  │  Consumer   │                                                 │
│  └──────┬──────┘                                                 │
│         │                                                        │
│         ▼                                                        │
│  ┌─────────────┐    ┌──────────────────────────────┐             │
│  │ Idempotence │───▶│  OCR Engine (Strategy)       │             │
│  │   Check     │    │  - TesseractEngine           │             │
│  └─────────────┘    │  - PaddleEngine (futur)      │             │
│         │           └──────────────────────────────┘             │
│         │                       │                                │
│         │                       ▼                                │
│         │           ┌─────────────┐                              │
│         │           │   MinIO     │  ← download(storage_key)     │
│         │           │  Download   │                              │
│         │           └─────────────┘                              │
│         │                                                        │
│         ▼                                                        │
│  ┌─────────────┐                                                 │
│  │ PostgreSQL  │  ← INSERT ocr_results                           │
│  │  (results)  │  ← INSERT processed_events                      │
│  └─────────────┘                                                 │
│         │                                                        │
│         ▼                                                        │
│  ┌─────────────┐                                                 │
│  │   Kafka     │  → documents.ocr.completed                      │
│  │  Producer   │                                                 │
│  └─────────────┘                                                 │
│                                                                  │
│  ┌─────────────────────────────────────────────────┐             │
│  │           REST API (FastAPI)                    │             │
│  │  GET /api/ocr/{documentId}                      │             │
│  │  GET /health                                    │             │
│  │  GET /metrics (Prometheus)                      │             │
│  └─────────────────────────────────────────────────┘             │
└──────────────────────────────────────────────────────────────────┘
```

---

## 🗄️ Schéma PostgreSQL prévu

### Table `ocr_results`

```sql
CREATE TABLE ocr_results (
    id            UUID PRIMARY KEY,
    document_id   UUID NOT NULL UNIQUE,
    storage_key   VARCHAR(500) NOT NULL,
    extracted_text TEXT,
    page_count    INT,
    ocr_engine    VARCHAR(50) NOT NULL,    -- 'tesseract', 'paddle'
    confidence    NUMERIC(5,2),             -- 0.00 - 100.00
    duration_ms   INT,
    status        VARCHAR(20) NOT NULL,     -- 'success', 'failed'
    error_message TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_ocr_document_id ON ocr_results(document_id);
CREATE INDEX idx_ocr_status ON ocr_results(status);
```

### Table `processed_events` (idempotence)

```sql
CREATE TABLE processed_events (
    event_id      VARCHAR(255) PRIMARY KEY,    -- Kafka headers eventId
    document_id   UUID NOT NULL,
    processed_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    status        VARCHAR(20) NOT NULL          -- 'success', 'failed', 'skipped'
);
CREATE INDEX idx_processed_document_id ON processed_events(document_id);
```

### Future migration (Phase 10)

```sql
-- Ajout pgvector pour recherche sémantique
CREATE EXTENSION vector;
ALTER TABLE ocr_results ADD COLUMN text_embedding vector(1536);
CREATE INDEX ON ocr_results USING ivfflat (text_embedding vector_cosine_ops);
```

---

## 📋 Plan détaillé — 12 sous-phases

### Phase 8.1 — Bootstrap (~1h)

**Branche** : `feat/ocr-bootstrap`

- [ ] Créer le dossier `services/ocr-service/`
- [ ] `uv init` (Python 3.12)
- [ ] Structure : `app/`, `tests/`, `alembic/`
- [ ] FastAPI hello world : `GET /` + `GET /health`
- [ ] Configuration via `pydantic-settings` (12-factor)
- [ ] Dockerfile multi-stage (builder + runtime)
- [ ] `.dockerignore`
- [ ] `pyproject.toml` complet
- [ ] Tests basiques pytest
- [ ] README ocr-service initial
- [ ] Commit + PR + merge

### Phase 8.2 — Database (~1h)

**Branche** : `feat/ocr-database`

- [ ] Ajouter PostgreSQL 16 au `docker-compose.infra.yml`
- [ ] SQLAlchemy 2.0 + Alembic setup
- [ ] Modèle `OcrResult` + `ProcessedEvent`
- [ ] Première migration Alembic (équivalent Flyway V1)
- [ ] Test connexion BDD
- [ ] Commit + PR + merge

### Phase 8.3 — Kafka consumer (~1h)

**Branche** : `feat/ocr-kafka-consumer`

- [ ] Lib : `aiokafka` (async) ou `confluent-kafka-python`
- [ ] Consumer du topic `documents.uploaded`
- [ ] Parsing du payload (record DTO Pydantic)
- [ ] Pour l'instant : juste **logger** ce qu'on reçoit
- [ ] Test bout-en-bout : upload via document-service → log dans ocr-service
- [ ] Commit + PR + merge

### Phase 8.4 — MinIO download (~1h)

**Branche** : `feat/ocr-storage`

- [ ] Lib : `minio-py` (SDK officiel)
- [ ] Service `StorageService.download(storage_key) -> bytes`
- [ ] Gestion erreurs (key not found, permission, etc.)
- [ ] Test : récupérer un fichier uploadé via document-service
- [ ] Commit + PR + merge

### Phase 8.5 — OCR Tesseract (~2h)

**Branche** : `feat/ocr-tesseract`

- [ ] Installation Tesseract (Dockerfile : `apt install tesseract-ocr tesseract-ocr-fra`)
- [ ] Lib : `pytesseract` + `pdf2image` (pour les PDF)
- [ ] **Strategy pattern** :
  ```python
  class OcrEngine(Protocol):
      async def extract_text(self, file_bytes: bytes, content_type: str) -> OcrResult

  class TesseractEngine: ...
  class PaddleEngine: ...   # Phase 9
  ```
- [ ] Implémentation `TesseractEngine`
- [ ] Tests unitaires sur fichiers samples
- [ ] Commit + PR + merge

### Phase 8.6 — Idempotence + persistence (~1h)

**Branche** : `feat/ocr-idempotence`

- [ ] `IdempotencyService.is_processed(event_id) -> bool`
- [ ] Workflow complet : Kafka → check idempotence → download → OCR → save BDD
- [ ] Si event déjà processed → skip + log
- [ ] Tests sur replay d'event
- [ ] Commit + PR + merge

### Phase 8.7 — Kafka publisher (~1h)

**Branche** : `feat/ocr-publisher`

- [ ] Producer Kafka (aiokafka)
- [ ] Event `documents.ocr.completed` avec record schema
- [ ] Publish après succès OCR + persist BDD
- [ ] **Outbox pattern simple** ou publish direct (à débattre — pour MVP : direct)
- [ ] Tests : verify message publié
- [ ] Commit + PR + merge

### Phase 8.8 — REST API (~1h)

**Branche** : `feat/ocr-api`

- [ ] `GET /api/ocr/{documentId}` → renvoie le texte extrait
- [ ] `GET /api/ocr/{documentId}/status` → status pipeline
- [ ] Auth JWT Keycloak (équivalent Spring `oauth2ResourceServer`)
- [ ] Lib : `python-jose` ou `pyjwt` + middleware FastAPI
- [ ] OpenAPI auto-doc via FastAPI
- [ ] Tests
- [ ] Commit + PR + merge

### Phase 8.9 — Observabilité (~1h)

**Branche** : `feat/ocr-observability`

- [ ] Lib : `prometheus-client`
- [ ] Métriques :
  - `ocr_documents_processed_total` (counter)
  - `ocr_documents_failed_total` (counter)
  - `ocr_processing_duration_seconds` (histogram)
  - `ocr_text_length_chars` (histogram)
- [ ] Endpoint `/metrics`
- [ ] Healthcheck custom (BDD + Kafka + MinIO)
- [ ] Logs structurés (structlog) avec `correlation_id`
- [ ] Commit + PR + merge

### Phase 8.10 — Tests Testcontainers (~2h)

**Branche** : `feat/ocr-tests`

- [ ] Lib : `testcontainers` (Python)
- [ ] PostgreSQL container
- [ ] MinIO container
- [ ] Kafka container (apache/kafka:3.8.0)
- [ ] Tests d'intégration end-to-end
- [ ] Coverage > 50%
- [ ] Commit + PR + merge

### Phase 8.11 — CI/CD GitHub Actions (~1h)

**Branche** : `feat/ci-cd-ocr`

- [ ] Workflow `.github/workflows/ocr-service-ci.yml`
- [ ] Jobs analogues à document-service :
  - `secrets-scan` (gitleaks)
  - `test` (pytest + coverage)
  - `quality` (ruff + mypy + black)
  - `security` (pip-audit ou safety)
  - `trivy-scan`
- [ ] Mise à jour CodeQL pour Python (`languages: ['java-kotlin', 'python']`)
- [ ] Mise à jour Dependabot (ajouter `pip` ecosystem)
- [ ] Commit + PR + merge

### Phase 8.12 — Deploy VPS (~2h)

**Branche** : `feat/deploy-vps-staging`

- [ ] `docker-compose.staging.yml` complet (infra + document-service + ocr-service)
- [ ] Configuration secrets via `.env` (jamais commité)
- [ ] Dockerfile production-ready (non-root user, healthcheck)
- [ ] Push images sur Docker Hub ou GitHub Container Registry
- [ ] Script de deploy sur VPS (`scripts/deploy-staging.sh`)
- [ ] Test end-to-end staging : upload → OCR → search
- [ ] Reverse proxy Traefik (futur HTTPS Let's Encrypt)
- [ ] Commit + PR + merge

---

## ⏱️ Estimation totale

| Phase | Durée |
|-------|-------|
| 8.1 → 8.10 | ~12h (code) |
| 8.11 → 8.12 | ~3h (CI/CD + deploy) |
| **Total** | **~15h** étalées sur plusieurs sessions |

---

## ✅ Critères d'acceptation Phase 8

À la fin, le système doit :

- [ ] Upload PDF via document-service → résultat OCR récupérable via ocr-service en < 30s
- [ ] Idempotence : re-consommer un event Kafka ne crée pas de doublon
- [ ] Tests verts en CI/CD (pytest + Testcontainers)
- [ ] Couverture > 50%
- [ ] Métriques Prometheus exposées
- [ ] Healthcheck `/health` opérationnel
- [ ] Déployé en staging sur VPS Contabo
- [ ] README ocr-service complet
- [ ] **Mêmes standards qualité que document-service** :
  - pre-commit hooks
  - gitleaks
  - Conventional Commits
  - Dependabot
  - CodeQL
  - Trivy

---

## 🎁 Bonus possibles (post Phase 8)

- **Phase 9** : Swap Tesseract → PaddleOCR (le Strategy pattern paye)
- **Phase 10** : Recherche sémantique (pgvector + embeddings via ollama ou OpenAI)
- **Phase 11** : search-service (Elasticsearch + indexation)
- **Phase 12** : Gateway (Spring Cloud Gateway + auth centralisée)
- **Phase 13** : Frontend React/Next.js
- **Phase 14** : Production sur VPS avec Traefik + SSL Let's Encrypt
- **Phase 15** : Kubernetes (k3s sur VPS, ou OVH Public Cloud)

---

## 📞 Première action de la nouvelle conversation

Dans la nouvelle conversation Claude, dire simplement :

> *"Salut, on reprend OCR Platform. On attaque Phase 8.1 — bootstrap ocr-service. Je suis dans `C:\ProjetML\ocr-platform`, branche `develop` à jour. Prêt."*

Claude devrait alors proposer :

1. Création de la branche `feat/ocr-bootstrap`
2. Init du projet Python avec uv
3. Structure de base FastAPI
4. Dockerfile
5. Premier commit

**Étape par étape, validation entre chaque.**
