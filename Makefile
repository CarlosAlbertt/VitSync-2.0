# Atajos del monorepo (guía, Fase 2.3).
# En Windows, si no tienes make, ejecuta el comando de la derecha tal cual.

.PHONY: up down logs api test seed migrate-info clean help

help:            ## Lista los comandos disponibles
	@grep -E '^[a-z-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}'

up:              ## Levanta Postgres + MinIO + Mailpit
	docker compose up -d

down:            ## Para los servicios (conserva los datos)
	docker compose down

clean:           ## Para los servicios y BORRA los volúmenes
	docker compose down -v

logs:            ## Sigue los logs de los servicios
	docker compose logs -f

api:             ## Arranca la API con el perfil local
	cd apps/api && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local

test:            ## Tests del backend (requiere Docker: Testcontainers)
	cd apps/api && ./mvnw verify

migrate-info:    ## Estado de las migraciones de Flyway
	cd apps/api && ./mvnw flyway:info
