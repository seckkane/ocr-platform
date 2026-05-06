# ADR 0001 — Adoption d'une architecture microservices

- **Statut** : Accepté
- **Date** : 2026-04-29
- **Décideurs** : Équipe technique

## Contexte

Le projet OCR Platform doit gérer trois domaines fonctionnels distincts :
1. **Gestion documentaire** (upload, stockage)
2. **Extraction OCR** (traitement intensif, écosystème Python/ML)
3. **Recherche full-text** (indexation Elasticsearch)

Ces domaines ont des caractéristiques très différentes :
- **Stack technique** : Java pour le métier, Python pour le ML
- **Profil de charge** : l'OCR est CPU-intensive, la recherche memory-intensive
- **Scalabilité** : besoin de scaler indépendamment chaque domaine

## Décision

Nous adoptons une **architecture microservices événementielle** avec :
- Un microservice par domaine fonctionnel
- Communication **asynchrone** via Apache Kafka entre services
- Communication **synchrone** (HTTP) uniquement pour les requêtes utilisateur (via Gateway)

## Alternatives considérées

### 1. Monolithe modulaire (rejeté)
- ✅ Plus simple à déployer
- ❌ Imposerait un seul langage (Java ou Python)
- ❌ Pas de scalabilité indépendante
- ❌ Couplage fort entre les modules

### 2. Microservices avec communication synchrone (REST entre services) (rejeté)
- ✅ Plus simple conceptuellement
- ❌ Couplage temporel fort (un service down = chaîne cassée)
- ❌ L'OCR pouvant durer 30s, l'utilisateur attendrait
- ❌ Pas de rejeu en cas d'échec

## Conséquences

### Positives
- Stack technique **adaptée à chaque domaine** (Java + Python)
- **Scalabilité indépendante** par service
- **Résilience** : un service down n'affecte pas les autres
- **Évolutivité** : ajout futur de services sans impact (ex: notification, audit)

### Négatives (à anticiper)
- **Complexité opérationnelle** accrue (plus de services à monitorer)
- **Cohérence à terme** (eventual consistency) au lieu de transactionnelle
- **Debugging distribué** plus complexe (besoin de tracing : OpenTelemetry)
- **Idempotence** des consumers Kafka à gérer rigoureusement

## Notes

- Le tracing distribué sera ajouté en Phase 4 (OpenTelemetry).
- Le pattern **Outbox** sera étudié pour garantir la cohérence entre la BDD et la publication d'events.
