# Backend Spring Boot (UTP)

API REST en Kotlin + Spring Boot 4, con **MySQL**, autenticación **JWT** y roles (ALUMNO, DOCENTE, JEFATURA). Es un proyecto Gradle independiente de la app Android.

## Requisitos
- JDK 17 o superior.
- MySQL 8 corriendo en `localhost:3306` (XAMPP, MySQL Workbench o Docker).

## Base de datos
El backend crea la base `utp_db` y las tablas solas (`createDatabaseIfNotExist` + `ddl-auto=update`) y la llena con datos de prueba. Solo necesitas un usuario con permisos. Variables de entorno (todas opcionales):

| Variable | Por defecto |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `utp_db` |
| `DB_USER` / `DB_PASSWORD` | `root` / (vacía) |
| `JWT_SECRET` | clave de desarrollo (cámbiala fuera de clase) |
| `STORAGE_DIR` | `./uploads` (aquí se guardan los archivos subidos) |

Ejemplo (Windows PowerShell): `$env:DB_PASSWORD="mi_clave"; ./gradlew bootRun`

## Ejecutar
```bash
cd backend
./gradlew bootRun        # Windows: gradlew.bat bootRun
```
Queda en `http://localhost:8080`. Los tests usan H2 en memoria (no necesitan MySQL).

## Usuarios de prueba (clave `123456`)
`alumno@utp.edu` (ALUMNO) · `docente@utp.edu` (DOCENTE) · `jefatura@utp.edu` (JEFATURA / Jefe UTP)

## Seguridad
`POST /api/auth/login` devuelve `{ id, nombre, email, rol, token, expiraEn }`. El resto de la API exige `Authorization: Bearer <token>` (sin token → 401, rol insuficiente → 403). El token dura 12 h.

## Endpoints

| Método | Ruta | Rol | Descripción |
|---|---|---|---|
| POST | `/api/auth/login` | público | Inicia sesión |
| GET | `/api/unidades` , `/api/unidades/{id}` | cualquiera | Unidades |
| POST | `/api/unidades` | DOCENTE, JEFATURA | Crear unidad |
| PUT/PATCH | `/api/unidades/{id}/estado` | JEFATURA | Aprobar / rechazar unidad |
| GET | `/api/recursos?estado=` | cualquiera | Alumno: solo autorizados · Docente: los suyos · Jefatura: todos |
| POST | `/api/recursos` (multipart: `titulo`, `descripcion`, `asignatura`, `unidadId`, `archivo`) | DOCENTE, JEFATURA | Subir material (máx. 50 MB). Docente → PENDIENTE; Jefatura → APROBADO |
| PUT/PATCH | `/api/recursos/{id}/estado` | JEFATURA | Autorizar (`APROBADO`) o rechazar (`RECHAZADO`) con observaciones |
| GET | `/api/recursos/{id}/archivo` | según permiso | Descarga del archivo |
| GET | `/api/progreso/mio` | ALUMNO | Avance (%) |
| POST | `/api/progreso/recursos/{id}` | ALUMNO | Marca un material como visto |
| GET | `/api/progreso/alumnos` | DOCENTE, JEFATURA | Avance de todos los alumnos |
| GET | `/api/evaluaciones/{unidadId}/preguntas` | cualquiera | Preguntas (sin la respuesta correcta) |
| POST | `/api/unidades/{unidadId}/preguntas` | DOCENTE, JEFATURA | Crear pregunta |
| POST | `/api/evaluaciones/{unidadId}/responder` | ALUMNO | `{ respuestas:[{preguntaId, opcion}] }` → puntaje |
| GET | `/api/evaluaciones/mis-resultados` | ALUMNO | Resultados del alumno |
| GET | `/api/tareas`, `/api/clases`, `/api/historial` | cualquiera | Panel e historial |

**Avance** = (materiales vistos + evaluaciones rendidas) / (materiales autorizados + unidades con evaluación).

## Probar con Postman
1. Levanta el backend.
2. **Import** → `backend/postman/UTP-Backend.postman_collection.json`.
3. En la carpeta *1. Autenticación* ejecuta los tres logins: cada uno guarda su token (`tokenAlumno`, `tokenDocente`, `tokenJefatura`).
4. En *Subir material* elige un archivo en el campo `archivo`. Luego prueba autorizar (Jefatura) y listar/descargar (Alumno). Las peticiones marcadas "espera 403/401" comprueban la seguridad.

## App Android
`ApiClient.kt` consume esta API (`ApiConfig.BASE_URL`, por defecto `http://10.0.2.2:8080` para el emulador; en celular físico usa la IP del PC y agrégala a `res/xml/network_security_config.xml`).
- **Navegación segura:** la sesión (token + rol) se guarda; cada ruta valida el rol y si el token vence o el servidor responde 401 se vuelve al login.
- **Docente / Jefe UTP:** panel "Material" para subir archivos; Jefatura autoriza o rechaza.
- **Estudiante:** pantalla Contenido con barra de avance, descarga de material al teléfono (se abre sin internet) y evaluaciones.

## Limitaciones
- Objetivos de aprendizaje, actividades y pruebas finales siguen solo en la app.
- Sin conexión el alumno ve y abre lo descargado, pero avance y evaluaciones requieren servidor.
- Los archivos subidos quedan en disco (`STORAGE_DIR`), no dentro de MySQL (solo sus datos).
inicio:
export DB_USER="root"
export DB_PASSWORD=""
./gradlew bootRun

"$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe" reverse tcp:8080 tcp:8080
