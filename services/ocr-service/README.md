\# 🔍 ocr-service



> Microservice Python d'extraction de texte depuis documents (PDF/images) via OCR.

> Fait partie de la plateforme \[OCR Platform](../../README.md).



\## 📊 Statut



🚧 \*\*Phase 8.1 — Bootstrap\*\* — FastAPI hello world + structure projet.



\## 🛠️ Stack



| Composant | Choix |

|-----------|-------|

| Runtime | Python 3.12 |

| Build/Deps | uv |

| Web framework | FastAPI |

| Tests | pytest + Testcontainers (Phase 8.10) |

| OCR | Tesseract → PaddleOCR (Phase 8.5 / 9) |

| BDD | PostgreSQL 16 (Phase 8.2) |

| Messaging | Kafka via aiokafka (Phase 8.3) |

| Storage | MinIO (Phase 8.4) |



\## 🚀 Démarrage local



\### Prérequis



\- Python 3.12+

\- \[uv](https://docs.astral.sh/uv/) installé



\### Installation



```bash

uv sync --group dev

```



\### Lancer le serveur



```bash

uv run uvicorn app.main:app --reload --port 8082

```



API disponible sur http://localhost:8082 :

\- \*\*Swagger UI\*\* : http://localhost:8082/swagger-ui.html

\- \*\*Health\*\* : http://localhost:8082/health



\### Lancer les tests



```bash

uv run pytest                                    # tests

uv run pytest --cov=app --cov-report=term-missing   # avec coverage

```



\### Qualité du code



```bash

uv run ruff check app tests        # linter

uv run ruff format app tests       # formatter

uv run mypy app                    # type checking

```



\## 🐳 Docker



```bash

docker build -t ocr-service:dev .

docker run -p 8082:8082 ocr-service:dev

```



\## 📁 Structure
