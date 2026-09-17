## Laboratorio – Parte 1: REST API Blueprints (Java 21 / Spring Boot 3.3.x)
# Escuela Colombiana de Ingeniería – Arquitecturas de Software

API REST para gestionar planos (*blueprints*) y sus puntos, con persistencia en **PostgreSQL**, respuestas uniformes `ApiResponse<T>`, documentación **OpenAPI/Swagger** y filtros de puntos activables por perfiles de Spring.

---

## 📋 Requisitos
- Java 21
- Maven 3.9+
- Docker (para PostgreSQL)

## ▶️ Ejecución del proyecto

1. Levantar PostgreSQL (crea la base `blueprints` con usuario/clave `blueprints`):
   ```bash
   docker compose up -d
   ```
2. Ejecutar la aplicación:
   ```bash
   mvn spring-boot:run
   ```
   La conexión se configura en `src/main/resources/application.yml` y se puede sobrescribir con las variables `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`. Las tablas se crean automáticamente (`spring.jpa.hibernate.ddl-auto=update`).

3. Activar un filtro de puntos (opcional):
   ```bash
   mvn spring-boot:run -Dspring-boot.run.profiles=redundancy
   mvn spring-boot:run -Dspring-boot.run.profiles=undersampling
   ```

## 🧪 Pruebas
```bash
mvn test
```
Las pruebas no requieren PostgreSQL: usan H2 en memoria (perfil `test`).

| Clase | Qué verifica |
|-------|--------------|
| `BlueprintsAPIControllerTest` | Códigos HTTP (200/201/202/400/404) y envoltorio `ApiResponse` con MockMvc |
| `PostgresBlueprintPersistenceTest` | Contrato de `BlueprintPersistence` sobre JPA: guardar, duplicados, búsquedas, orden de puntos |
| `RedundancyFilterTest`, `UndersamplingFilterTest` | Lógica de los filtros |
| `BlueprintsSmokeTest` | El contexto de Spring arranca completo |

---

## 🌐 Endpoints (`/api/v1/blueprints`)

| Método | Ruta | Éxito | Error |
|--------|------|-------|-------|
| GET | `/api/v1/blueprints` | `200 OK` | – |
| GET | `/api/v1/blueprints/{author}` | `200 OK` | `404` autor sin blueprints |
| GET | `/api/v1/blueprints/{author}/{bpname}` | `200 OK` (aplica el filtro activo) | `404` no existe |
| POST | `/api/v1/blueprints` | `201 Created` | `400` datos inválidos o ya existe |
| PUT | `/api/v1/blueprints/{author}/{bpname}/points` | `202 Accepted` | `404` no existe |

Todas las respuestas usan el mismo envoltorio:
```json
{
  "code": 200,
  "message": "OK",
  "data": { "author": "john", "name": "house", "points": [ { "x": 0, "y": 0 } ] }
}
```
En los errores `data` es `null` y `message` describe la causa.

Ejemplos con `curl`:
```bash
curl -s http://localhost:8080/api/v1/blueprints | jq
curl -s http://localhost:8080/api/v1/blueprints/john | jq
curl -s http://localhost:8080/api/v1/blueprints/john/house | jq
curl -i -X POST http://localhost:8080/api/v1/blueprints -H 'Content-Type: application/json' \
  -d '{ "author":"john","name":"kitchen","points":[{"x":1,"y":1},{"x":2,"y":2}] }'
curl -i -X PUT http://localhost:8080/api/v1/blueprints/john/kitchen/points -H 'Content-Type: application/json' \
  -d '{ "x":3,"y":3 }'
```

## 📖 Documentación y monitoreo
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- Actuator: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health), `/actuator/metrics`

Evidencia en base de datos (tras crear un blueprint):
```bash
docker exec -it blueprints-postgres psql -U blueprints -d blueprints \
  -c "SELECT b.author, b.name, p.position, p.x, p.y FROM blueprints b JOIN points p ON p.blueprint_id = b.id ORDER BY b.id, p.position;"
```

---

## 🗂️ Estructura (arquitectura por capas)

```
src/main/java/edu/eci/arsw/blueprints
  ├── model/         # Dominio: Blueprint, Point
  ├── dto/           # ApiResponse<T> (respuesta uniforme)
  ├── persistence/   # Interfaz BlueprintPersistence + InMemory y Postgres (JPA)
  │    └── entity/   # Entidades JPA y repositorio Spring Data
  ├── services/      # Lógica de negocio (BlueprintsServices)
  ├── filters/       # Identity, Redundancy, Undersampling (perfiles de Spring)
  ├── controllers/   # BlueprintsAPIController (/api/v1/blueprints)
  ├── exception/     # GlobalExceptionHandler (excepciones → códigos HTTP)
  └── config/        # OpenApiConfig
```

## ✅ Buenas prácticas aplicadas
- **Versionamiento** de la API en la ruta (`/api/v1`) para poder evolucionar sin romper clientes.
- **Respuesta uniforme** `ApiResponse<T>` en éxitos y errores; los clientes siempre reciben la misma forma.
- **Códigos HTTP semánticos** y **manejo centralizado de errores** con `@RestControllerAdvice`: el controlador no captura excepciones; el dominio lanza `BlueprintNotFoundException` / `BlueprintPersistenceException` y el *handler* las traduce a `404` / `400`.
- **Validación declarativa** del cuerpo (`@Valid`, `@NotBlank`) con respuesta `400` que indica el campo inválido.
- **Persistencia intercambiable**: `PostgresBlueprintPersistence` implementa el mismo contrato `BlueprintPersistence` que la versión en memoria y se activa con `@Primary`; el servicio y el controlador no cambian. El modelo de dominio se mantiene separado de las entidades JPA.
- **Filtros por perfil**: `IdentityFilter` solo se registra si no hay un perfil de filtro activo, evitando ambigüedad de beans.
- **Configuración externalizable** vía variables de entorno y **pruebas sin infraestructura** (H2) para que `mvn test` funcione en cualquier máquina o CI.
