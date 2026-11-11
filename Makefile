# Atajos del monorepo (guía, Fase 2.3).
# En Windows, si no tienes make, ejecuta el comando de la derecha tal cual.

.PHONY: up down logs api api-empty web web-mocks test migrate-info clean help

help:            ## Lista los comandos disponibles
	@grep -E '^[a-z-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}'

up:              ## Levanta Postgres (vitsync y vitsync_dev) + MinIO + Mailpit
	docker compose up -d

down:            ## Para los servicios (conserva los datos)
	docker compose down

clean:           ## Para los servicios y BORRA los volúmenes
	docker compose down -v

logs:            ## Sigue los logs de los servicios
	docker compose logs -f

api:             ## Arranca la API con los datos de demostración (base vitsync_dev)
	cd apps/api && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local,demo

api-empty:       ## Arranca la API sobre la base vacía (vitsync), para probar desde cero
	cd apps/api && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

web:             ## Frontend contra la API local; solo se simula lo que aún no tiene endpoint
	corepack pnpm --filter web dev

web-mocks:       ## Frontend con todo simulado, sin backend ni Docker
	corepack pnpm --filter web dev:mocks

test:            ## Tests del backend (requiere Docker: Testcontainers) y del frontend
	cd apps/api && ./mvnw verify
	corepack pnpm --filter web test

migrate-info:    ## Estado de las migraciones de Flyway
	cd apps/api && ./mvnw flyway:info
