# MS-TRANSFER-LIMITS

Microservicio para consultar límites, actualizarlos y validar/consumir el acumulado diario por cuenta y canal.

## Requisitos previos

- JDK 25
- Maven 3.9+
- Docker y Docker Compose (para PostgreSQL/Jaeger)

## Ejecutar local

```bash
docker compose up -d postgres jaeger
mvn spring-boot:run
```

OpenAPI: `openapi.yaml`. Swagger UI: `http://localhost:8080/swagger-ui/index.html`. Jaeger UI: `http://localhost:16686`. Salud: `http://localhost:8080/actuator/health`.

## Ejemplo rápido

```bash
curl -X PUT http://localhost:8080/api/v1/accounts/acc-123/limits/MOBILE_APP \
  -H 'Content-Type: application/json' \
  -d '{"dailyLimit":1000.00,"perOperationLimit":600.00}'

curl -X POST http://localhost:8080/api/v1/accounts/acc-123/transfer-validations \
  -H 'Content-Type: application/json' \
  -d '{"channel":"MOBILE_APP","amount":250.00}'

curl http://localhost:8080/api/v1/accounts/acc-123/limits
```

## Construir, formato y cobertura

```bash
mvn -B verify
mvn fmt:format
```

`verify` verifica formato Google Java, pruebas unitarias y al menos 90% de cobertura de líneas de la lógica de aplicación y API. Clases de arranque, persistencia JPA y configuración declarativa se excluyen del umbral porque son infraestructura de framework. No hay pruebas de integración ni escaneo de dependencias, según el alcance solicitado. El reporte se genera en `target/site/jacoco/index.html`.

## Instalar bloqueo pre-push

```bash
scripts/install-hooks.sh
```

El hook de pre-commit revisa formato y el de pre-push ejecuta `mvn -B verify`; si falla, Git cancela la operación. Para repositorios clonados sin hooks habilitados, cada desarrollador debe correr el instalador. GitHub no ejecuta hooks locales por sí mismo, por lo que una rama protegida debe exigir también el workflow de CI antes de aceptar cambios.

## Docker

```bash
docker compose up --build
```

El contenedor de la aplicación se ejecuta como usuario sin privilegios. Cambia las credenciales de ejemplo antes de cualquier despliegue compartido y proporciona secretos mediante el entorno/secret manager.

## Arquitectura y decisiones

Consulta el contrato [openapi.yaml](openapi.yaml). El código sigue capas `api` → `application` → `domain` → `infrastructure`. Para validaciones simultáneas se bloquea la fila de la cuenta/canal (`SELECT ... FOR UPDATE` vía `@Lock(PESSIMISTIC_WRITE)`) dentro de una única transacción PostgreSQL, de modo que dos validaciones concurrentes sobre la misma cuenta/canal se serializan y ninguna puede exceder el límite diario. El acumulado diario usa fecha UTC (`Clock` inyectable). La autenticación/autorización no está implementada y debe aplicarse en el gateway o añadirse antes de exponer el servicio a clientes.

## Agente utilizado y estrategia de trabajo

**Agente:** GitHub Copilot (agente de código, modelo Claude Sonnet) ejecutado dentro de Visual Studio Code, apoyado por:

- `CLAUDE.md`: reglas obligatorias de ingeniería (Google Java Format, capas, RFC 9457, bloqueo de fila, Apache Commons Text, OpenTelemetry, cobertura, sin pruebas de integración ni escaneo de dependencias).
- `.github/skills/java-service-reviewer/SKILL.md`: skill propia que guía la revisión paso a paso de cualquier cambio (validación → lógica de negocio → persistencia → OpenAPI).
- `.github/agents/code-security-reviewer.agent.md`: subagente de solo lectura invocado para revisar calidad de código, concurrencia, vulnerabilidades e inyección de datos antes de dar por cerrado un cambio.
- `scripts/pre-commit` y `scripts/pre-push` (instalados con `scripts/install-hooks.sh`): hooks de Git que bloquean el commit/push si falla el formato Google o `mvn -B verify` (tests + cobertura 90%).
- `.github/workflows/ci.yml`: verificación equivalente en GitHub Actions para ramas protegidas, ya que los hooks locales no se ejecutan del lado del servidor.

**Estrategia paso a paso:**

1. Se definió el contrato (`openapi.yaml`) con los 4 endpoints requeridos y errores RFC 9457 antes de escribir código.
2. Se diseñó el dominio (`LimitPolicy`, `TransferChannel`, `TransferDecision`, `LimitViolation`) separado de la infraestructura JPA.
3. Se implementó la persistencia (`AccountChannelLimitEntity`/`Repository` + migración Flyway `V1__create_account_channel_limits.sql`) con bloqueo pesimista de fila para proteger el consumo concurrente del límite diario.
4. Se implementó `LimitService` con las reglas de negocio (límite por operación, límite diario acumulado, mensajes de rechazo armados con Apache Commons Text `StringSubstitutor`).
5. Se expuso la capa `api` (`LimitController`, `ApiExceptionHandler`) traduciendo violaciones de dominio a `ProblemDetail` (RFC 9457).
6. Se añadió OpenTelemetry (starter + exportador OTLP hacia Jaeger en `compose.yaml`) y Actuator para `/actuator/health`.
7. Se escribieron pruebas unitarias de `LimitService`, `LimitController` y `ApiExceptionHandler` hasta cumplir el umbral de cobertura del 90% configurado en `jacoco-maven-plugin`, excluyendo clases de arranque/infraestructura declarativa.
8. Se configuró el harness de protección: `CLAUDE.md`, skill, subagente revisor, hooks de pre-commit/pre-push y CI, además de `.vscode/settings.json` para formatear con el estilo Google al guardar.
9. Se empaquetó la imagen Docker multi-stage (build con Maven, runtime JRE, usuario sin privilegios) y `compose.yaml` para levantar PostgreSQL, Jaeger y el servicio juntos.
10. Se ejecutó `mvn -B verify` de forma iterativa para confirmar formato, pruebas y cobertura antes de cada entrega.
