# Guía de despliegue: MrElote en Render + Supabase

## 1. Supabase — preparación

> **Resumen del flujo de auth en esta app** (importante para entender por qué algunos pasos están y otros NO):
> - **Admin seed**: `DataInitializer` usa la **Admin API** (`POST /auth/v1/admin/users` con `email_confirm: true`) → admin queda activo sin confirmación de email. Solo se ejecuta en el primer boot.
> - **Registro de clientes**: `AuthService.register` usa el **signup público** (`POST /auth/v1/signup`) → Supabase envía email de confirmación. El cliente queda inactivo hasta hacer click en el link. Row local en `usuarios` se inserta en ese mismo paso (existe en BD pero no puede hacer login hasta confirmar).
> - **Login**: `AuthService.login` exige row en `usuarios` + credenciales válidas en Supabase. Si el usuario no confirmó, Supabase responde 400 (`email_not_confirmed`) y el backend hoy lo mapea a "Credenciales inválidas" (ver deuda en sección 9).
> - **JWT**: validados vía **JWKS asimétrica** (`jwk-set-uri`). Requiere firma RS256/ES256 (no HS256).
> - **Postgres**: el backend se conecta como **usuario dedicado** (`MRELOTE_SUPABASE_DB_USER`). Flyway corre en cada boot y crea el esquema.
> - **Rol** (ADMIN/CLIENTE) NO viene en el JWT: se resuelve leyendo la tabla local `usuarios` (`SecurityConfig#authoritiesFor`).

### 1.1. Crear proyecto

En [supabase.com](https://supabase.com). Anota `project_ref` (ej `abcxyz1234567`). Región: la más cercana a Render (US East / N. Virginia es buen match con Render Oregon/Ohio).

### 1.2. Habilitar firma asimétrica JWT (obligatorio)

El backend está configurado con `jwk-set-uri` (ver `application.yaml`). Si el proyecto Supabase fue creado antes de la migración a JWT signing keys, los tokens siguen siendo HS256 y Spring no podrá verificarlos.

- Dashboard → **Project Settings → Auth → JWT Keys** (`/dashboard/project/<ref>/settings/auth`).
- Si aparece un botón **"Migrate JWT secret" / "Rotate keys"**, ejecútalo para activar firma asimétrica.
- Verifica:
  ```bash
  curl https://<ref>.supabase.co/auth/v1/.well-known/jwks.json
  ```
  Debe devolver al menos una clave con `kid` y `kty: RSA` (alg `RS256`) o `kty: EC` (alg `ES256` con `crv: P-256`).

- **El algoritmo del JWK debe matchear `spring.security.oauth2.resourceserver.jwt.jws-algorithms`** en `application.yaml`. Default: `ES256` (override con env `MRELOTE_SUPABASE_JWS_ALG`).
  - Supabase con **ECC P-256** → `ES256` (default, listo).
  - Supabase con **RSA** → setea `MRELOTE_SUPABASE_JWS_ALG=RS256`.
  - Si no coinciden, todas las llamadas autenticadas devuelven 401 con `Signed JWT rejected: Another algorithm expected`.

> **Riesgo R6**: si tu plan / versión de proyecto NO expone JWT signing keys asimétricos (queda HS256), el deploy queda bloqueado hasta migrar.

### 1.3. Crear usuario Postgres dedicado (con permisos completos)

> **Por qué dedicado:** el backend NO debe usar el rol `postgres` del proyecto (privilegios excesivos + esa contraseña es la de toda la base). Un usuario propio limita el blast radius.

Ejecuta en **SQL Editor** del dashboard (el SQL Editor corre como `postgres`, dueño del schema `public`):

```sql
-- 1) Crear rol
CREATE USER mrelote_app WITH PASSWORD 'STRONG_PASSWORD_AQUI';

-- 2) Permitir entrar y crear objetos en el schema public
GRANT USAGE, CREATE ON SCHEMA public TO mrelote_app;

-- 3) Privilegios sobre objetos existentes (idempotente — útil si Flyway ya corrió)
GRANT ALL PRIVILEGES ON ALL TABLES    IN SCHEMA public TO mrelote_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO mrelote_app;

-- 4) Privilegios sobre objetos FUTUROS creados por postgres en public
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL ON TABLES    TO mrelote_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT ALL ON SEQUENCES TO mrelote_app;
```

**Notas críticas:**
- `GRANT CREATE ON SCHEMA public` es **imprescindible**: sin él, Flyway falla con `permission denied for schema public` cuando intenta correr `V1__init_schema.sql` (Supabase revocó el CREATE por default en `public` desde 2024).
- Como Flyway corre como `mrelote_app`, las tablas que cree quedarán **owned por `mrelote_app`** — perfecto, no requiere transferir ownership.
- Cambia `STRONG_PASSWORD_AQUI` por una password fuerte sin caracteres que requieran URL-encoding (`@`, `:`, `/`, `?`, `#`) para evitar problemas con el JDBC URL. Si los necesitas, codifícalos.

### 1.4. Configurar signup público con confirmación por email (obligatorio)

El registro de clientes va vía `/auth/v1/signup` y debe enviar correo de confirmación. Pasos:

1. **Habilitar signup público.**
   - Dashboard → **Authentication → Sign In / Up → Allow new users to sign up: ON**.
   - Sin esto, `AuthService.register` falla con 422.

2. **Activar "Confirm email".**
   - Dashboard → **Authentication → Providers → Email → Confirm email: ON**.
   - Con esto activo, `/auth/v1/signup` devuelve el `User` sin sesión y Supabase manda el correo de verificación. Mientras no confirme, `signInWithPassword` responde 400 (`email_not_confirmed`).

3. **Configurar Site URL y Redirect URLs.**
   - Dashboard → **Authentication → URL Configuration**.
   - El frontend envía `redirectTo = <origin>/confirm.html` al registrar; el backend lo reenvía a Supabase como query `redirect_to`. Supabase exige que esa URL esté en la whitelist o el link del correo cae al **Site URL** por default.
   - **Site URL**: `https://<mrelote-frontend>.onrender.com/confirm.html` (fallback de prod).
   - **Additional Redirect URLs** (whitelist):
     - `http://localhost:8081/confirm.html` (dev local).
     - `https://<mrelote-frontend>.onrender.com/confirm.html` (prod, si no es la Site URL).
     - Alternativamente wildcards: `http://localhost:8081/*` y `https://<mrelote-frontend>.onrender.com/*`.
   - Sin estas entradas en la whitelist, Supabase ignora el `redirect_to` y manda al Site URL.

4. **Email templates (opcional).**
   - Dashboard → **Authentication → Email Templates → Confirm signup**.
   - El template default funciona. Si quieres personalizarlo, conserva la variable `{{ .ConfirmationURL }}`.

5. **SMTP — DECISIÓN PENDIENTE (ver sección 9).**
   - Por default Supabase usa su SMTP built-in con límite estricto (≈ 4 correos/hora por proyecto en plan free). Aceptable para dev, **NO para producción**.
   - Producción debe usar SMTP propio (Resend, SendGrid, SES, etc.) configurado en **Authentication → SMTP Settings**.

### 1.5. Hardening Supabase

1. **NO activar Row Level Security en las tablas del schema `public`.**
   - Verifica en **Database → Tables → (cada tabla del proyecto: `usuarios`, `categorias`, ...)** que **"Enable RLS"** está **OFF**.
   - Justificación: el backend se conecta como `mrelote_app` (rol custom de Postgres), NO como el rol `authenticated` que Supabase espera para evaluar políticas RLS. Activar RLS rompería todos los queries de la app.

2. **Política de password — sincronizada.**
   - Dashboard → **Authentication → Sign In / Up → Password requirements**: debe ser **"Lower, upper, digit, symbol" + mínimo 8**.
   - `RegisterRequest` valida con regex `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$` (mismo criterio).
   - El frontend valida con la misma regex antes de enviar (`js/auth.js`).
   - `MRELOTE_ADMIN_PASSWORD` también debe cumplir esta política (sembrado vía Admin API).

### 1.6. Connection string

El backend se conecta a Postgres directamente vía JDBC. Hay dos topologías posibles según dónde corra el backend:

> **Nota:** `MRELOTE_SUPABASE_DB_HOST` lleva el **JDBC URL completo** (host+port+db+params en un solo valor). No hay vars separadas de PORT/NAME.

**Opción A — Conexión directa (local dev, hosts con IPv6):**
- JDBC URL: `jdbc:postgresql://db.<ref>.supabase.co:5432/postgres?sslmode=require`
- User: `mrelote_app`
- Password: la que pusiste en 1.3.

**Opción B — Session Pooler (Render y cualquier host SIN IPv6 saliente):**
- Database → **Connect → Session pooler**.
- JDBC URL: `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require`
- User: el dashboard muestra el formato `<user>.<project_ref>`. **Caveat (no validado en este proyecto):** históricamente Supavisor solo aceptó el user `postgres.<ref>`. Si `mrelote_app.<ref>` falla con `Tenant or user not found`, hay dos salidas:
  1. (preferida) Verifica en el dashboard de Database → Roles si el rol custom se replica al pooler; si sí, usa `mrelote_app.<ref>`.
  2. (fallback, menos seguro) Usa `postgres.<ref>` con la contraseña del proyecto. Esto significa privilegios de superuser para la app — **NO recomendado** salvo bloqueo.
- Verifica el username exacto que muestra el dashboard.

> **Riesgo R1**: Render no soporta IPv6 saliente en planes free/standard; la conexión directa (`db.<ref>.supabase.co`) resuelve a IPv6 y falla. **En Render, usa siempre Opción B.**

> **TLS:** Supabase exige TLS. Incluí `?sslmode=require` directamente en `MRELOTE_SUPABASE_DB_HOST`.

### 1.7. Recoge credenciales

| Variable | Valor |
|----------|-------|
| `SUPABASE_URL` | `https://<ref>.supabase.co` |
| `SUPABASE_ANON_KEY` | Settings → API → `anon public` |
| `SUPABASE_SERVICE_ROLE_KEY` | Settings → API → `service_role` **(secreto, NO commitear)** |
| `SUPABASE_JWKS_URI` | `https://<ref>.supabase.co/auth/v1/.well-known/jwks.json` |
| `SUPABASE_ISSUER` | `https://<ref>.supabase.co/auth/v1` |
| `DB_HOST` / `DB_USER` / `DB_PASSWORD` | Según opción A o B de 1.5 |

---

## 2. Ajustes previos al deploy (backend)

### 2.1. Dockerfile path — APLICADO

El Dockerfile vive en `mrelote/mrelote/Dockerfile` (movido desde `mrelote/Dockerfile`). Render usará Root Directory = `mrelote/mrelote`, Dockerfile Path = `Dockerfile`.

### 2.2. Bind a `$PORT` y `sslmode=require` — APLICADO

`mrelote/mrelote/src/main/resources/application.yaml` ya contiene:

```yaml
server:
  port: ${PORT:8080}
  servlet:
    context-path: /api/v1
spring:
  datasource:
    url: ${MRELOTE_SUPABASE_DB_HOST}
```

Render setea `PORT` (default 10000); el backend bindea ahí. El JDBC URL completo (incluyendo `?sslmode=require`) viaja en `MRELOTE_SUPABASE_DB_HOST`.

### 2.3. Cookies cross-domain — decisión clave

Opciones:

- **A (recomendada, mismo origen efectivo)**: front como Static Site con rewrite `/api/v1/*` → backend. Browser ve mismo origen → cookies same-site funcionan con `SameSite=Strict`. Cero cambios en config.
- **B (dominios separados)**: cambia `MRELOTE_COOKIE_SAME_SITE=None` + `MRELOTE_COOKIE_SECURE=true`. `ALLOWED_ORIGINS=https://<front>.onrender.com`. Browser exige HTTPS (Render lo da) y `SameSite=None`.

Recomendada: **A**.

## 3. Render — Backend (Web Service Docker)

### 3.1. Crear servicio

Dashboard → **New → Web Service → Connect repo**.

### 3.2. Configuración

| Campo | Valor |
|-------|-------|
| Name | `mrelote-backend` |
| Region | misma que Supabase (Oregon/Ohio) |
| Branch | `main` |
| Root Directory | `mrelote/mrelote` |
| Language | `Docker` |
| Dockerfile Path | `Dockerfile` (relativo al root dir) |
| Health Check Path | (vacío inicialmente — agregar Actuator después si se necesita) |
| Instance Type | Starter o superior (Free duerme tras 15 min) |

### 3.3. Environment Variables

Una por una en el panel "Environment". Marca **Secret** en las sensibles.

```
MRELOTE_SUPABASE_URL              = https://<ref>.supabase.co
MRELOTE_SUPABASE_ANON_KEY         = <anon_key>
MRELOTE_SUPABASE_SERVICE_ROLE_KEY = <service_role_key>   [Secret]
MRELOTE_SUPABASE_JWKS_URI         = https://<ref>.supabase.co/auth/v1/.well-known/jwks.json
MRELOTE_SUPABASE_ISSUER           = https://<ref>.supabase.co/auth/v1
MRELOTE_SUPABASE_DB_HOST          = jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
MRELOTE_SUPABASE_DB_USER          = mrelote_app.<ref>     # ver caveat sección 1.6
MRELOTE_SUPABASE_DB_PASSWORD      = <password>           [Secret]
MRELOTE_ACCESS_COOKIE_NAME        = mre_access
MRELOTE_REFRESH_COOKIE_NAME       = mre_refresh
MRELOTE_COOKIE_DOMAIN             = (vacío)
MRELOTE_COOKIE_SECURE             = true
MRELOTE_COOKIE_SAME_SITE          = Strict     (Opción A) | None (Opción B)
MRELOTE_ALLOWED_ORIGINS           = https://<front>.onrender.com
MRELOTE_ADMIN_NOMBRE              = Administrador
MRELOTE_ADMIN_TELEFONO            = 0000000000
MRELOTE_ADMIN_CORREO              = admin@mrelote.com
MRELOTE_ADMIN_PASSWORD            = <password cumpliendo política Supabase>  [Secret]
MRELOTE_ADMIN_DIRECCION           = MrElote HQ
```

### 3.4. Deploy

Espera build (3-5 min). Logs deben mostrar Flyway migrando `V1__init_schema.sql` y `DataInitializer` creando admin.

### 3.5. Verificación

URL backend: `https://mrelote-backend.onrender.com`.

```bash
curl https://mrelote-backend.onrender.com/api/v1/business/status
```

→ 200 OK.

---

## 4. Render — Frontend (Static Site con rewrite)

### 4.1. Crear sitio

Dashboard → **New → Static Site → Connect repo**.

### 4.2. Configuración

| Campo | Valor |
|-------|-------|
| Name | `mrelote-frontend` |
| Branch | `main` |
| Root Directory | `mrelote-front` |
| Build Command | (vacío — no hay build) |
| Publish Directory | `.` |

### 4.3. Redirects/Rewrites

Settings → Redirects & Rewrites → Add Rule:

| Source | Destination | Action |
|--------|------------|--------|
| `/api/v1/*` | `https://mrelote-backend.onrender.com/api/v1/*` | Rewrite |

Browser ve las llamadas como mismo-origen (proxy server-side de Render).

### 4.4. Headers (opcional, recomendado)

Settings → Custom Headers:

```
Path: /*
Headers:
  X-Content-Type-Options: nosniff
  X-Frame-Options: DENY
  Referrer-Policy: strict-origin-when-cross-origin
```

### 4.5. Deploy

URL: `https://mrelote-frontend.onrender.com`.

### 4.6. Verificación

Navega → DevTools → Network → request a `/api/v1/business/status` → Status 200. Misma URL host = same-origin.

---

## 5. Actualiza `MRELOTE_ALLOWED_ORIGINS`

Una vez tengas la URL del front, vuelve al backend → Environment → actualiza:

```
MRELOTE_ALLOWED_ORIGINS = https://mrelote-frontend.onrender.com
```

Guardar → Render redespliega.

Con Opción A (rewrite) el browser siempre llama al **mismo origen del front**, así que el header `Origin` será `https://mrelote-frontend.onrender.com`.

---

## 6. Smoke test

1. Abre `https://mrelote-frontend.onrender.com`.
2. Login admin (`admin@mrelote.com` / password). DevTools → Application → Cookies: deben aparecer `mre_access` y `mre_refresh` como `HttpOnly` + `Secure`.
3. Crear categoría/producto → 200.
4. Logout → cookies se borran.
5. Registro de cliente (flujo con email confirm):
   1. Desde el front, registra un cliente nuevo con un correo real (al que tengas acceso).
   2. Verifica en **Supabase Dashboard → Authentication → Users** que aparece con `Email confirmed: NO`.
   3. Verifica en tabla `usuarios` (Database → Tables) que la row local existe con `rol = CLIENTE`.
   4. Intenta login antes de confirmar → debe fallar (hoy con mensaje "Credenciales inválidas"; ver R10 / sección 9).
   5. Abre el correo, click en el link de confirmación → debe redirigir a la URL configurada en Site URL / Redirect URLs.
   6. Verifica en Supabase que `Email confirmed` pasó a `YES`.
   7. Login → ahora debe entrar y emitir cookies.

---

## 7. Operación

- **Flyway**: corre en cada boot. Migraciones nuevas → agregar `V2__*.sql` en `mrelote/mrelote/src/main/resources/db/migration/` y push.
- **Service role key**: NO commitear, solo en Render env. Si se filtra → Supabase dashboard → reset.
- **Free plan**: backend duerme tras 15 min sin tráfico. Primer request tras dormir tarda 30-60s. Si molesta, sube a Starter ($7/mes).
- **Logs**: Render Dashboard → service → Logs (en vivo). Si el seeding del admin falla aparece `no se pudo crear admin en Supabase` — revisa env vars.

---

## 8. Riesgos / pendientes

- **R1**: Render free outbound IPv6 no soportado → **Session Pooler es obligatorio**, no la conexión directa.
- **R2**: Health check Render sin endpoint dedicado solo verifica que el puerto responda. Para health real: agregar `spring-boot-starter-actuator` + path `/api/v1/actuator/health` (con `management.endpoint.health.access=read-only` y `management.endpoints.web.exposure.include=health`).
- **R3**: Free static site soporta rewrite a cualquier URL pública. Rewrite a otro Render service no es interno — pasa por internet pública. Latencia extra ~50-100 ms. Aceptable para dev.
- **R4**: `MreloteApplicationTests` está `@Disabled` → `./gradlew build` no falla en Render. Si quieres tests reales, monta proyecto test separado en Supabase con creds en `application-test.yaml`.
- **R5**: `MRELOTE_COOKIE_SECURE=true` requiere HTTPS (Render lo da por default). Localmente sigue siendo `false`.
- **R6 (bloqueante)**: si tu plan Supabase no expone "JWT signing keys" (firma asimétrica), no podrás cumplir el requisito JWKS. Verifícalo en Settings → JWT antes de avanzar.
- **R7**: el rol custom `mrelote_app` no ha sido validado contra el Session Pooler de Supavisor en este proyecto. Si falla con `Tenant or user not found`, ver fallback en sección 1.6.
- **R8**: `GRANT CREATE ON SCHEMA public` es **obligatorio** desde Supabase 2024+. Si lo olvidas, Flyway falla en el primer boot con `permission denied for schema public`.
- **R9**: si alguien activa Row Level Security en cualquier tabla del schema `public`, la app deja de funcionar (el rol `mrelote_app` no es `authenticated`). Documentado en 1.5.
- **R10**: `AuthService.login` mapea genéricamente 400/401 a "Credenciales inválidas". Cuando un usuario intenta entrar sin confirmar email, Supabase responde 400 (`email_not_confirmed`) pero el cliente recibe el mensaje genérico → UX confusa. Manejo específico pendiente (ver sección 9).
- **R11**: si un usuario se registra y nunca confirma, queda una row huérfana en `usuarios` y otra en `auth.users` (no confirmada). Hoy NO hay job de limpieza. Documentado en sección 9.
- **R12**: SMTP built-in de Supabase tiene rate limit estricto (free tier ≈ 4 correos/hora). Si haces más de N registros seguidos, los correos siguientes se pierden. Producción debe usar SMTP propio.

---

## 9. Decisiones pendientes / fuera de alcance

Items que **NO** se cierran en esta guía por requerir validación o quedar fuera del alcance funcional actual:

- **Validación empírica del pooler con user custom** (R7). Hacer la prueba antes del primer deploy productivo y actualizar 1.6 con la conclusión.
- **Endpoint de health real** (R2). Hoy Render solo verifica que el puerto responda.
- **Tests de integración con Supabase** (R4). `MreloteApplicationTests` sigue `@Disabled`.
- **Endurecer `MRELOTE_ADMIN_PASSWORD`** del default `Admin1234!` antes de exponer públicamente (cumple la regex mínima pero es predecible).
- **SMTP propio para producción** (R12). Default Supabase tiene rate limit estricto. Decidir proveedor (Resend / SendGrid / SES) y configurar.
- **Manejo específico de `email_not_confirmed`** en `AuthService.login` (R10). Hoy se mapea a "Credenciales inválidas" — UX confusa.
- **Limpieza de huérfanos no confirmados** (R11). Definir TTL para `auth.users` no confirmados + estrategia para borrar también la row local de `usuarios`.
- **Auto-login después de confirmación**: hoy `confirm.html` muestra "cuenta confirmada" y exige login manual. Los tokens que Supabase pone en el fragment NO se intercambian por cookies HttpOnly. Implementar `POST /auth/session-from-tokens` si se quiere auto-login.
