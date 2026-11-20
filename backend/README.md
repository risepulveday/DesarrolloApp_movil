# Backend Spring Boot (UTP)

API REST en Kotlin + Spring Boot que reemplaza los datos locales de la app (SQLite en `UTPDatabaseHelper` y listas en memoria en `UtpRepository`).

Es un proyecto Gradle **independiente** de la app Android: no está incluido en el `settings.gradle.kts` raíz, para no mezclar el plugin de Spring Boot con el de Android.

## Requisitos
- JDK 17 o superior (Spring Boot 4 no corre con Java 11).

## Ejecutar
Abre la carpeta `backend/` como proyecto aparte en IntelliJ IDEA / Android Studio, o desde terminal:

```bash
cd backend
./gradlew bootRun        # Windows: gradlew.bat bootRun
```

Queda en `http://localhost:8080`. La base H2 se guarda en `backend/data/` y se llena sola con los mismos datos de prueba de la app. Consola de BD: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:file:./data/utpdb`, usuario `sa`, sin contraseña).

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/login` | `{ "email", "password" }` → `{ id, nombre, email, rol }` (401 si es inválido) |
| GET | `/api/tareas` | Tareas del alumno |
| GET | `/api/clases` | Clases del docente |
| GET | `/api/unidades?asignatura=&estado=` | Lista de unidades (filtros opcionales) |
| GET | `/api/unidades/{id}` | Una unidad |
| POST | `/api/unidades` | Crear unidad |
| PATCH | `/api/unidades/{id}/estado` | `{ "estado", "observaciones", "usuario" }` — cambia estado y registra auditoría |
| GET | `/api/historial?entidadId=` | Historial de cambios |

Usuarios de prueba (clave `123456`): `alumno@utp.edu`, `docente@utp.edu`, `jefatura@utp.edu`.

Prueba rápida:
```bash
curl -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"docente@utp.edu","password":"123456"}'
```

## Conectar la app Android (pendiente)
1. En `AndroidManifest.xml`: agregar `<uses-permission android:name="android.permission.INTERNET" />` y, solo en desarrollo, `android:usesCleartextTraffic="true"` en `<application>`.
2. Desde el emulador, el servidor de tu PC es `http://10.0.2.2:8080/` (no `localhost`). En un celular físico usa la IP de tu PC en la red local.
3. Agregar Retrofit (o Ktor client) en `libs.versions.toml` y reemplazar las llamadas a `UTPDatabaseHelper` / `UtpRepository` por llamadas a estos endpoints.

## Limitaciones actuales
- No hay token/JWT: el login valida credenciales pero el resto de endpoints no está protegido.
- Solo están las unidades, el historial, tareas, clases y usuarios. Objetivos de aprendizaje, actividades y pruebas finales siguen solo en la app.
