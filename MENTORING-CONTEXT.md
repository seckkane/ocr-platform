# 🎓 OCR Platform — Mentoring Context

> **À LIRE EN PREMIER** — Ce document définit le cadre de travail entre l'utilisateur (Issa) et Claude (mentor). À uploader dans le projet Claude pour que toutes les conversations partagent ce contexte.

---

## 👤 Le développeur

**Issa Seck Kane**
- 📧 issaseckkane@gmail.com
- 💼 LinkedIn : https://www.linkedin.com/in/issaseckkane
- 🐙 GitHub : https://github.com/seckkane
- 🌍 Localisation : Sénégal (Dakar)
- 🗣️ Langue de communication : **Français** (impérative)

### Profil technique

- **Niveau** : Senior Java/Spring (expérience hôpital en production)
- **Expérience CI/CD** : a déjà utilisé GitHub Actions + DockerHub en prod
- **Veut monter en compétence sur** :
  - Architectures microservices et event-driven
  - Python (FastAPI, OCR)
  - Culture DevSecOps complète
  - Kubernetes (plus tard, post Phase 8)

### Environnement de travail

- **OS** : Windows 11
- **Terminal** : PowerShell 5.1 (pas PowerShell 7+)
- **IDE** : IntelliJ IDEA Community 2025.2.4
- **Java** : 21 (Temurin)
- **Python** : 3.12.13 + uv
- **Docker** : Docker Desktop 28.x
- **Path projet** : `C:\ProjetML\ocr-platform`

### VPS de déploiement

- **Provider** : Contabo
- **IP** : 207.180.240.150
- **OS** : Ubuntu 24.04
- **Specs** : 4 vCPU, 7.8 GB RAM
- **Docker** : 29.2.1 installé
- **État** : Actuellement CLEAN (containers staging supprimés)

---

## 🎯 Style de mentoring attendu

### Ce qu'Issa préfère

- ✅ **Concis et direct** — pas de blabla, droit au but
- ✅ **Théorie courte** avant le code (pourquoi avant comment)
- ✅ **Code prêt à coller** dans des blocs complets
- ✅ **Plusieurs `-m` pour les git commit** (Windows PowerShell oblige)
- ✅ **Approche progressive** — étape par étape, validation entre chaque
- ✅ **Décisions seniors expliquées** (pourquoi pas X, pourquoi Y)
- ✅ **Conventions strictes** : Conventional Commits, Git Flow, etc.

### Ce qu'Issa n'aime pas

- ❌ Réponses trop longues
- ❌ Plusieurs concepts mélangés dans une seule étape
- ❌ Boilerplate ou flatteries inutiles
- ❌ Avancer sans valider que ça marche

### Pattern d'interaction efficace

1. Claude propose → théorie courte + code à coller
2. Issa exécute + colle les outputs
3. Claude valide ou debug → étape suivante

---

## 🪟 Pièges Windows/PowerShell maîtrisés

Ces problèmes ont déjà été résolus, à garder en tête :

### Encodage de fichiers

- `Out-File -Encoding utf8` ajoute un **BOM** (`EF BB BF`) qui casse les YAML strict
- ❌ Ne pas utiliser : `notepad` génère parfois des CRLF
- ✅ Utiliser : `[System.IO.File]::WriteAllText("$PWD\file.yml", $content, [System.Text.UTF8Encoding]::new($false))`
- ✅ Ou : Notepad avec **Encoding = "UTF-8"** (PAS "UTF-8 with BOM")

### Pre-commit hooks LF/CRLF

- Le hook `mixed-line-ending` corrige automatiquement les fins de ligne
- **Cycle typique** : commit → hook fixe → re-stage → re-commit
- Pour pré-fixer : `pre-commit run --all-files` avant le commit

### PowerShell here-strings

- Le `@"..."@` ne marche pas toujours via copy-paste
- Si le here-string casse → utiliser Notepad

### Git multi-line commits

- ❌ `git commit -m "ligne 1\nligne 2"` ne fonctionne pas
- ✅ Utiliser plusieurs `-m` :
  ```powershell
  git commit `
    -m "feat: titre" `
    -m "Description ligne 1" `
    -m "Description ligne 2"
  ```

### Maven Wrapper sur Windows

- `mvnw.cmd` (Windows) ET `mvnw` (Linux/Mac) **doivent tous deux être commités**
- Sans `mvnw`, la CI Linux échoue avec "No such file"
- Le `.gitignore` ne doit PAS ignorer `.mvn/` ni `mvnw*`

### IntelliJ caches désynchros

- Après changements Lombok ou structure projet → **File → Invalidate Caches and Restart**
- Avant tests : `mvn clean` obligatoire après changements d'annotations

### PATH pour outils uv

- À chaque nouvelle session PowerShell :
  ```powershell
  $env:PATH = "$env:USERPROFILE\.local\bin;$env:PATH"
  ```

### gitleaks v8.22.1 bug

- Crash WASM sur Windows : `panic: wasm error: invalid table access`
- Fix : downgrade à `v8.18.4` dans `.pre-commit-config.yaml`

---

## 🏗️ Le projet — OCR Platform

### Vision business

Plateforme distribuée d'extraction et indexation de texte depuis documents (PDF/images) :
1. Upload via API authentifiée
2. OCR automatique (Tesseract puis PaddleOCR)
3. Recherche full-text + sémantique

### Architecture cible

```
Client → Gateway (Keycloak auth)
           ↓
    ┌──────┼──────┐
    ↓      ↓      ↓
document-svc → ocr-service → search-service
    ↓          ↓                ↓
  MinIO    PostgreSQL    Elasticsearch
  MySQL    (pgvector)
    ↓          ↑                ↑
    └──→ Kafka events ──────────┘
```

### Stack globale

- **Java 21** + **Spring Boot 3.5.14** : document-service, search-service, gateway
- **Python 3.12** + **FastAPI** + **Tesseract** : ocr-service
- **Apache Kafka 3.8.0** (mode KRaft) : messaging async
- **MinIO** : stockage S3-compatible
- **MySQL 8.4** : metadata document-service
- **PostgreSQL 16** : ocr-service (pgvector ready)
- **Elasticsearch 8** : search-service (futur)
- **Keycloak 26** : auth OIDC/JWT
- **Prometheus + Grafana** : observabilité
- **Docker Compose** : dev local
- **GitHub Container Registry (GHCR)** : images Docker
- **VPS Contabo** : staging + futur prod

### Repo GitHub

- **URL** : https://github.com/seckkane/ocr-platform
- **Visibilité** : PUBLIC (volontaire — repo portfolio)
- **Branches protégées** : `main` + `develop` (PR obligatoire, no force push, linear history)
- **Auto-delete merged branches** : ✅ activé

### Conventions

- **Git Flow simplifié** :
  - `main` = production
  - `develop` = intégration (PR depuis feat/)
  - `feat/<nom>` = nouvelles features
  - `hotfix/<nom>` = corrections prod urgentes
  - `docs/<nom>` = documentation pure
- **Conventional Commits** OBLIGATOIRE (validé par hook `commit-msg`)
  - Préfixes acceptés : `feat`, `fix`, `chore`, `docs`, `style`, `refactor`, `perf`, `test`, `build`, `ci`, `revert`

---

## 📊 État actuel du projet

### Document-service : ✅ PRODUCTION-READY + STAGING-READY

Voir le fichier `DOCUMENT-SERVICE-COMPLETED.md` pour le détail technique complet.

**Dernières évolutions** (mai 2026) :
- ✅ Phase A : 4 profils Spring (dev/test/staging/prod) avec hardening progressif
- ✅ Quick-win sécurité : TestControllers en `@Profile("dev")` (whitelist)
- ✅ Phase B.1-B.3 : Dockerfile prod-ready + docker-compose.staging.yml + workflow GHCR
- 🚧 Phase B.4 (deploy VPS) : reportée APRÈS Phase 8 (ocr-service) pour déployer l'ensemble cohérent en une fois

**État GHCR** : image `ghcr.io/seckkane/ocr-platform/document-service:develop` disponible, build automatique à chaque push develop.

### OCR-service : 🚧 À DÉMARRER (Phase 8)

Voir le fichier `OCR-SERVICE-PHASE-8-PLAN.md` pour le plan détaillé.

**Décision senior** : on bootstrap ocr-service AVANT de finaliser le déploiement VPS de document-service. Raison : on déploiera la stack complète (document + ocr) en une fois, pas service par service. Évite de jeter du travail si l'archi évolue.

### Search-service : 📋 PLANIFIÉ

Phase ultérieure, après ocr-service.

---

## 🚀 Commandes pour reprendre

```powershell
# Aller dans le projet
cd C:\ProjetML\ocr-platform

# Activer pre-commit (si nouvelle session)
$env:PATH = "$env:USERPROFILE\.local\bin;$env:PATH"

# Démarrer infra
docker compose -f docker-compose.infra.yml up -d
docker compose -f docker-compose.infra.yml ps

# Run document-service
cd services/document-service
.\mvnw.cmd spring-boot:run

# Tests rapides
.\mvnw.cmd verify

# Tests complets (qualité)
.\mvnw.cmd verify -P quality

# Sync develop avant nouvelle feature
git checkout develop
git pull origin develop
git checkout -b feat/<nouvelle-feature> develop

# Pour reprendre Phase 8.1 (bootstrap ocr-service)
git checkout develop
git pull origin develop
git checkout -b feat/ocr-bootstrap develop
cd services
mkdir ocr-service
cd ocr-service
# Suivre OCR-SERVICE-PHASE-8-PLAN.md section Phase 8.1
```

---

## 🎯 Ce qu'attendre de Claude (mentor) dans une nouvelle conversation

Quand tu démarres une nouvelle conversation dans ce projet, dis simplement :

> *"Salut, on reprend OCR Platform. On attaque [Phase X] — [description courte]"*

Claude saura :
- Te parler en français
- Utiliser un style concis avec code prêt à coller
- Respecter Conventional Commits + Git Flow
- Connaître le contexte technique complet
- Proposer des étapes incrémentales avec validation entre chaque
