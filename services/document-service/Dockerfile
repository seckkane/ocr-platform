# ============================================================================
# document-service — Production-ready Dockerfile
# ----------------------------------------------------------------------------
# Multi-stage build pour minimiser la taille de l'image finale.
#
# Build :    docker build -t document-service:local services/document-service
# Run :      docker run -p 8081:8081 -e SPRING_PROFILES_ACTIVE=dev document-service:local
# Health :   docker inspect --format='{{.State.Health.Status}}' <container_id>
# ============================================================================

# ===========================================================================
# Stage 1 : BUILDER (Maven + JDK 21)
# ---------------------------------------------------------------------------
# Compile le JAR. Cette image n'est PAS embarquee dans le runtime final.
# ===========================================================================
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /build

# 1. Copier le Maven Wrapper et le pom.xml (cache invalide UNIQUEMENT si pom.xml change)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# 2. Pre-telecharger les dependances (cache Docker --> reutilise tant que pom.xml stable)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# 3. Copier le code source et compiler
COPY src ./src
RUN ./mvnw clean package -DskipTests -B \
    && cp target/*.jar app.jar

# ===========================================================================
# Stage 2 : RUNTIME (JRE 21 seul, pas de Maven, pas de sources)
# ---------------------------------------------------------------------------
# Image finale minimale (~400 MB).
# ===========================================================================
FROM eclipse-temurin:21-jre-jammy

# Metadata image (norme OCI)
LABEL org.opencontainers.image.title="document-service"
LABEL org.opencontainers.image.description="OCR Platform - Document upload and storage service"
LABEL org.opencontainers.image.source="https://github.com/seckkane/ocr-platform"
LABEL org.opencontainers.image.licenses="Internal"

WORKDIR /app

# Setup en un seul RUN (1 layer au lieu de 2) :
#   1. Creer un user non-root pour executer l'app (securite : si compromission, pas root sur le container)
#   2. Installer curl pour le HEALTHCHECK (Jammy minimal n'en a pas)
#   3. Cleanup apt cache pour reduire la taille de l'image
#
# Note : on ne pinne pas la version de curl car les versions Ubuntu LTS sont
# regulierement mises a jour pour patcher des CVE. Un pinning strict forcerait
# a maintenir manuellement la version a chaque rebuild de l'image de base.
# La regle hadolint DL3008 est donc volontairement ignoree sur ce RUN.
# hadolint ignore=DL3008
RUN groupadd -r spring \
    && useradd -r -g spring -d /app spring \
    && chown -R spring:spring /app \
    && apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Copier le JAR depuis le builder (--chown pour que spring puisse le lire)
COPY --from=builder --chown=spring:spring /build/app.jar app.jar

# Switcher sur le user non-root
USER spring

# Port expose (document-service ecoute sur 8081)
EXPOSE 8081

# Healthcheck Docker (utilise par docker compose et K8s)
# --start-period=60s : Spring Boot prend ~30-40s a demarrer, on laisse de la marge
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8081/actuator/health/liveness || exit 1

# Variables d'env JVM (override-ables au runtime via -e JAVA_OPTS=...)
# MaxRAMPercentage=75 : la JVM utilise 75% de la RAM du container (laisse 25% pour OS)
# +UseContainerSupport : active la detection des cgroups (active par defaut en Java 17+, on est explicite)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

# ENTRYPOINT en forme exec (PAS de shell wrapper) pour que SIGTERM passe a la JVM
# (sinon graceful shutdown casse, le container met 10s a tuer)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
