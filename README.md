# MrElote

Aplicación para gestión de pedidos con backend en Spring Boot + PostgreSQL y un frontend estático (HTML/JS).

## Estructura del repositorio

```
MrElote/
├── mrelote/              # Backend (Spring Boot 4 + Java 21 + Gradle)
│   ├── docker-compose.yml
│   ├── openapi.yaml
│   ├── MrElote.postman_collection.json
│   └── mrelote/          # Proyecto Gradle
└── mrelote-front/        # Frontend estático (HTML + CSS + JS vanilla)
```

## Requisitos

- **Java 21** (el `build.gradle` usa un toolchain, Gradle puede descargarlo automáticamente).
- **Docker** y **Docker Compose** (para la base de datos).
- Cualquier Navegador web

---

## 1) Levantar la base de datos

Desde `mrelote/`:

```bash
cd mrelote
docker compose up -d
```

Esto levanta un PostgreSQL 17 con:

| Campo    | Valor             |
|----------|-------------------|
| Host     | `localhost`       |
| Puerto   | `5432`            |
| DB       | `mrelote`         |
| Usuario  | `mrelote`         |
| Password | `mrelote`         |

Para verificar que está arriba:

```bash
docker ps
docker logs -f mrelote-db
```

### Conectarse a la BD

Con `psql` dentro del contenedor:

```bash
docker exec -it mrelote-db psql -U mrelote -d mrelote
```

O desde un cliente externo (DBeaver, TablePlus, DataGrip, pgAdmin) usando los datos de la tabla anterior.

Cadena JDBC:

```
jdbc:postgresql://localhost:5432/mrelote
```

Para apagar y borrar volumen (reset total de la BD):

```bash
docker compose down -v
```

---

## 2) Levantar el backend

Desde `mrelote/mrelote/`:

```bash
cd mrelote/mrelote
./gradlew bootRun           # Linux / macOS / Git Bash
gradlew.bat bootRun         # CMD/PowerShell
```

El backend queda escuchando en:

```
http://localhost:8080/api/v1
```

Configuración relevante (`src/main/resources/application.yaml`):

- `server.servlet.context-path: /api/v1`
- `spring.jpa.hibernate.ddl-auto: update` → Hibernate crea/actualiza el schema al arrancar.
- JWT con expiración de 24h (`app.jwt.expiration: 86400000`).
- Tarifa de envío por defecto: `3000`.

### Usuario administrador (seed)

Al arrancar, el backend crea/asegura un usuario admin con los valores de `app.admin` del `application.yaml`:

| Campo     | Valor                 |
|-----------|-----------------------|
| Correo    | `admin@mrelote.com`   |
| Password  | `admin1234`           |
| Rol       | `ADMIN`               |

Usa estas credenciales para entrar en la pantalla de login del frontend y acceder a `admin.html`.

### Probar la API

- OpenAPI: `mrelote/openapi.yaml`
- Colección Postman: `mrelote/MrElote.postman_collection.json`

Endpoints base:

- Auth: `POST /api/v1/auth/register`, `POST /api/v1/auth/login`
- Catálogo público: `GET /api/v1/categories`, `GET /api/v1/products`
- Admin: `/api/v1/admin/**` (requiere JWT con rol `ADMIN`)

---

## 3) Levantar el frontend

El frontend es estático y consume `http://localhost:8080/api/v1` (definido en `mrelote-front/js/api.js`).

Desde `mrelote-front/`:

Abre index.html


### Páginas disponibles

| Página          | Rol requerido | Descripción               |
|-----------------|---------------|---------------------------|
| `index.html`    | Público       | Login / registro          |
| `catalog.html`  | Cliente       | Catálogo de productos     |
| `cart.html`     | Cliente       | Carrito                   |
| `orders.html`   | Cliente       | Mis pedidos               |
| `admin.html`    | ADMIN         | Gestión (productos, categorías, pedidos, config) |

---

## 4) Flujo rápido de prueba

1. `docker compose up -d` en `mrelote/`.
2. `./gradlew bootRun` en `mrelote/mrelote/`.
4. Abrir `index.html`.
5. **Cliente**: registrarse desde el formulario → usar catálogo/carrito/pedidos.
6. **Admin**: entrar con `admin@mrelote.com` / `admin1234` → ir a `admin.html`.

---

## Solución de problemas

- **Puerto 5432 ocupado**: otro Postgres local está corriendo. Detenlo o cambia el mapeo en `docker-compose.yml` (`"5433:5432"`) y actualiza `application.yaml`.
- **Puerto 8080 ocupado**: añade `server.port: 8081` en `application.yaml` y actualiza `BASE_URL` en `mrelote-front/js/api.js`.
- **Resetear BD**: `docker compose down -v && docker compose up -d`. Con `ddl-auto: update` Hibernate recreará las tablas al reiniciar el backend.
- **Token expirado**: la sesión dura 24h; vuelve a hacer login desde `index.html`.
