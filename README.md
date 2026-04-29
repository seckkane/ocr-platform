# 📄 OCR Platform

> Plateforme de gestion documentaire avec OCR automatique et recherche full-text.
> Architecture microservices événementielle (event-driven).

---

## 🎯 Vue d'ensemble

OCR Platform est un système distribué permettant à un utilisateur de :
- **Uploader** des documents (PDF, images)
- **Extraire automatiquement** le texte via OCR (PDF natifs ou scannés)
- **Rechercher** dans le contenu des documents (full-text)

L'architecture suit les principes **microservices** et **event-driven**, avec une communication asynchrone via Apache Kafka.

---

## 🏗️ Architecture

```
                            ┌─────────────┐
                            │   CLIENT    │
                            └──────┬──────┘
                                   │ HTTPS
                            ┌──────▼──────┐
                            │   GATEWAY   │  Auth (Keycloak) · Routing · Rate Limit
                            │ Spring Boot │
                            └──┬───┬───┬──┘
                ┌──────────────┘   │   └──────────────┐
                │                  │                  │
        ┌───────▼────────┐ ┌───────▼────────┐ ┌───────▼────────┐
        │ document-svc   │ │   ocr-service  │ │  search-svc    │
        │  Spring Boot   │ │     FastAPI    │ │  Spring Boot   │
        │  Upload+MinIO  │ │ PaddleOCR/Tess │ │ Elasticsearch  │
        └───────┬────────┘ └────────┬───────┘ └────────▲───────┘
                │                   │                  │
                └─────────► Kafka ◄─┴──────────────────┘
                                    
        ┌──────────────────────────────────────────────────┐
        │                  INFRASTRUCTURE                  │
        │ MinIO · Kafka · Elasticsearch · MySQL · Redis    │
        │ Keycloak · Eureka · Prometheus · Grafana         │
        └──────────────────────────────────────────────────┘
```

### Flow de traitement d'un document

1. Le client uploade un PDF via la **Gateway** (authentifié par JWT Keycloak).
2. Le **document-service** stocke le fichier dans **MinIO**, les métadonnées dans **MySQL**, et publie un event `documents.uploaded` dans **Kafka**.
3. L'**ocr-service** consomme l'event, télécharge le fichier, extrait le texte (OCR si nécessaire) et publie `documents.ocr.completed`.
4. Le **search-service** consomme cet event et indexe le contenu dans **Elasticsearch**.
5. Le client peut alors rechercher dans le contenu via la **Gateway** → **search-service**.

---

## 📁 Structure du monorepo

```
ocr-platform/
├── services/                  # Microservices applicatifs
│   ├── document-service/      # Spring Boot — Upload & stockage
│   ├── ocr-service/           # FastAPI — Extraction OCR
│   ├── search-service/        # Spring Boot — Indexation & recherche
│   └── gateway/               # Spring Cloud Gateway
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
├── docker-compose.yml         # Stack complète (dev local)
└── docker-compose.infra.yml   # Infra uniquement (sans services)
```

---

## 🛠️ Stack technique

| Domaine | Technologie | Version |
|---------|-------------|---------|
| Langage backend | Java | 21 (LTS) |
| Langage OCR | Python | 3.11+ |
| Framework Java | Spring Boot | 3.3.x |
| Framework Python | FastAPI | 0.115+ |
| Build Java | Maven | 3.9+ |
| Stockage objet | MinIO | latest |
| Base de données | MySQL | 8.x |
| Recherche | Elasticsearch | 8.x |
| Messaging | Apache Kafka | 3.7+ |
| Cache | Redis | 7.x |
| Auth / SSO | Keycloak | 24.x |
| Service Discovery | Netflix Eureka | - |
| Monitoring | Prometheus + Grafana | - |
| Containerisation | Docker + Compose | - |

---

## 🚀 Démarrage rapide

### Prérequis

- Java 21+
- Maven 3.9+
- Python 3.11+
- Docker Desktop 24+
- Git

### Lancer l'infrastructure

```bash
docker compose -f docker-compose.infra.yml up -d
```

### Lancer un service

Voir le README spécifique de chaque service dans `services/<nom-du-service>/`.

---

## 📚 Documentation

- **Architecture détaillée** : [`docs/architecture/`](./docs/architecture/)
- **Décisions d'architecture (ADR)** : [`docs/adr/`](./docs/adr/)
- **API specs** : [`docs/api/`](./docs/api/)

---

## 🔄 Conventions

### Commits

Format **Conventional Commits** :
- `feat: ajout de l'upload multipart`
- `fix: correction du timeout MinIO`
- `docs: mise à jour du README`
- `refactor: extraction du service de stockage`
- `test: ajout des tests d'intégration upload`
- `chore: bump version Spring Boot`

### Branches

- `main` : branche stable (production-ready)
- `develop` : branche d'intégration
- `feat/<nom>` : nouvelle fonctionnalité
- `fix/<nom>` : correction de bug

---

## 📖 Auteur

Projet réalisé dans le cadre d'une montée en compétence sur les architectures microservices.

## 📄 Licence

À définir.