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
| PATCH o PUT | `/api/unidades/{id}/estado` | `{ "estado", "observaciones", "usuario" }` — cambia estado y registra auditoría (la app usa PUT) |
| GET | `/api/historial?entidadId=` | Historial de cambios |

Usuarios de prueba (clave `123456`): `alumno@utp.edu`, `docente@utp.edu`, `jefatura@utp.edu`.

Prueba rápida:
```bash
curl -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"docente@utp.edu","password":"123456"}'
```

## Conexión con la app Android
La app ya llama a este backend (`app/.../data/remote/ApiClient.kt`):
- **Login:** `LoginScreen` valida contra `/api/auth/login` (ya no usa `UTPDatabaseHelper`).
- **Unidades e historial:** `UtpRepository` los carga del servidor al iniciar sesión, y envía al servidor las aprobaciones/rechazos y las publicaciones nuevas. Si no hay conexión, queda el cambio local.
- **Dirección del servidor:** `ApiConfig.BASE_URL` (por defecto `http://10.0.2.2:8080`, el PC visto desde el emulador). En un celular físico usa la IP de tu PC y agrégala también en `app/src/main/res/xml/network_security_config.xml`.

Para probar: levanta el backend (`./gradlew bootRun` en `backend/`), luego corre la app en el emulador e ingresa con `jefatura@utp.edu` / `123456`.

## Limitaciones actuales
- No hay token/JWT: el login valida credenciales pero el resto de endpoints no está protegido.
- Solo están las unidades, el historial, tareas, clases y usuarios. Objetivos de aprendizaje, actividades y pruebas finales siguen solo en la app.
- Editar una unidad ya existente (`guardarOActualizarUnidad`) aún no se envía al servidor; tareas y clases tienen endpoint pero la app todavía no las muestra desde ahí.
