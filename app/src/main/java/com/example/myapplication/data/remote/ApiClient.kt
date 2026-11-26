package com.example.myapplication.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.myapplication.data.model.AsignaturaUTP
import com.example.myapplication.data.model.EstadoContenido
import com.example.myapplication.data.model.HistorialAuditoria
import com.example.myapplication.data.model.Material
import com.example.myapplication.data.model.PreguntaApi
import com.example.myapplication.data.model.Progreso
import com.example.myapplication.data.model.ResultadoApi
import com.example.myapplication.data.model.UnidadPedagogica
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object ApiConfig {
    // 10.0.2.2 es el PC anfitrión visto desde el emulador de Android Studio.
    // En un celular físico, cambiar por la IP de tu PC en la red local (ej. "http://192.168.1.50:8080")
    // y agregarla también en res/xml/network_security_config.xml.
    const val BASE_URL = "http://10.0.2.2:8080"
}

class ApiException(val code: Int, message: String) : IOException(message)

/**
 * Cliente del backend Spring Boot (carpeta backend/).
 * Usa HttpURLConnection + org.json, ambos incluidos en Android, para no agregar dependencias.
 * Todas las funciones son suspend y corren en Dispatchers.IO.
 * Cada petición lleva el token de la sesión (Authorization: Bearer ...).
 */
object ApiClient {

    private const val TIMEOUT_MS = 8000
    private const val TIMEOUT_ARCHIVOS_MS = 120000

    // ---------- Base HTTP ----------

    private fun abrir(method: String, path: String, readTimeout: Int = TIMEOUT_MS): HttpURLConnection {
        val conn = URL(ApiConfig.BASE_URL + path).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = readTimeout
        conn.setRequestProperty("Accept", "application/json")
        Session.token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
        return conn
    }

    /** Lee la respuesta; si el servidor responde con error lanza ApiException (401 cierra la sesión). */
    private fun leer(conn: HttpURLConnection): String {
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code !in 200..299) {
            if (code == 401) Session.cerrar() // token vencido o inválido: la navegación vuelve al login
            throw ApiException(code, text.ifBlank { "HTTP $code" })
        }
        return text
    }

    private fun call(method: String, path: String, body: JSONObject? = null): String {
        val conn = abrir(method, path)
        try {
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            return leer(conn)
        } finally {
            conn.disconnect()
        }
    }

    // ---------- Autenticación ----------

    /** Devuelve el rol ("ALUMNO", "DOCENTE", "JEFATURA") y deja la sesión iniciada; null si las credenciales son inválidas. */
    suspend fun login(email: String, password: String): String? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("email", email.trim()).put("password", password.trim())
            val o = JSONObject(call("POST", "/api/auth/login", body))
            Session.iniciar(
                token = o.getString("token"),
                rol = o.getString("rol"),
                nombre = o.getString("nombre"),
                userId = o.getLong("id"),
                expiraEn = o.getLong("expiraEn")
            )
            o.getString("rol")
        } catch (e: ApiException) {
            if (e.code == 401) null else throw e
        }
    }

    // ---------- Unidades e historial ----------

    suspend fun getUnidades(): List<UnidadPedagogica> = withContext(Dispatchers.IO) {
        val arr = JSONArray(call("GET", "/api/unidades"))
        List(arr.length()) { arr.getJSONObject(it).toUnidad() }
    }

    suspend fun getHistorial(): List<HistorialAuditoria> = withContext(Dispatchers.IO) {
        val arr = JSONArray(call("GET", "/api/historial"))
        List(arr.length()) { arr.getJSONObject(it).toHistorial() }
    }

    suspend fun crearUnidad(unidad: UnidadPedagogica) = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("asignatura", unidad.asignatura.name)
            .put("numero", unidad.numero)
            .put("titulo", unidad.titulo)
            .put("descripcion", unidad.descripcion)
            .put("estado", unidad.estado.name)
            .put("modificadoPor", unidad.modificadoPor)
        call("POST", "/api/unidades", body)
        Unit
    }

    // Se usa PUT porque HttpURLConnection de Android no soporta PATCH; el backend acepta ambos.
    suspend fun cambiarEstado(
        unidadId: Long,
        estado: EstadoContenido,
        observaciones: String?,
        usuario: String = "Jefatura UTP"
    ) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("estado", estado.name).put("usuario", usuario)
        if (observaciones != null) body.put("observaciones", observaciones)
        call("PUT", "/api/unidades/$unidadId/estado", body)
        Unit
    }

    // ---------- Material (archivos) ----------

    /** Alumno: material autorizado. Docente: el suyo + autorizado. Jefatura: todo. */
    suspend fun listarMaterial(estado: String? = null): List<Material> = withContext(Dispatchers.IO) {
        val query = if (estado != null) "?estado=$estado" else ""
        val arr = JSONArray(call("GET", "/api/recursos$query"))
        List(arr.length()) { Material.fromJson(arr.getJSONObject(it)) }
    }

    /** Sube un archivo elegido con el selector de archivos del teléfono. */
    suspend fun subirMaterial(
        context: Context,
        uri: Uri,
        titulo: String,
        descripcion: String?,
        asignatura: String?,
        unidadId: Long?
    ): Material = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val nombre = nombreDeArchivo(context, uri).replace("\"", "'")
        val tipo = resolver.getType(uri) ?: "application/octet-stream"
        val boundary = "----utp${System.currentTimeMillis()}"

        val conn = abrir("POST", "/api/recursos", TIMEOUT_ARCHIVOS_MS)
        try {
            conn.doOutput = true
            conn.setChunkedStreamingMode(0)
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            conn.outputStream.use { out ->
                fun campo(clave: String, valor: String) {
                    out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$clave\"\r\n\r\n$valor\r\n".toByteArray())
                }
                campo("titulo", titulo)
                if (!descripcion.isNullOrBlank()) campo("descripcion", descripcion)
                if (asignatura != null) campo("asignatura", asignatura)
                if (unidadId != null) campo("unidadId", unidadId.toString())
                out.write(
                    ("--$boundary\r\nContent-Disposition: form-data; name=\"archivo\"; filename=\"$nombre\"\r\n" +
                        "Content-Type: $tipo\r\n\r\n").toByteArray()
                )
                val entrada = resolver.openInputStream(uri) ?: throw IOException("No se pudo leer el archivo elegido")
                entrada.use { it.copyTo(out) }
                out.write("\r\n--$boundary--\r\n".toByteArray())
            }
            Material.fromJson(JSONObject(leer(conn)))
        } finally {
            conn.disconnect()
        }
    }

    /** Solo Jefatura: aprobar, rechazar o publicar material. */
    suspend fun cambiarEstadoMaterial(id: Long, estado: String, observaciones: String?): Material =
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("estado", estado)
            if (observaciones != null) body.put("observaciones", observaciones)
            Material.fromJson(JSONObject(call("PUT", "/api/recursos/$id/estado", body)))
        }

    /** Descarga el archivo al almacenamiento de la app para verlo sin conexión. */
    suspend fun descargarMaterial(id: Long, destino: File) = withContext(Dispatchers.IO) {
        val conn = abrir("GET", "/api/recursos/$id/archivo", TIMEOUT_ARCHIVOS_MS)
        try {
            conn.setRequestProperty("Accept", "*/*")
            val code = conn.responseCode
            if (code !in 200..299) {
                if (code == 401) Session.cerrar()
                val msg = conn.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw ApiException(code, msg.ifBlank { "HTTP $code" })
            }
            destino.parentFile?.mkdirs()
            val parcial = File(destino.parentFile, destino.name + ".part")
            conn.inputStream.use { entrada -> parcial.outputStream().use { entrada.copyTo(it) } }
            // Recién cuando termina completo se cambia al nombre final (no quedan archivos a medias)
            if (destino.exists()) destino.delete()
            if (!parcial.renameTo(destino)) {
                parcial.copyTo(destino, overwrite = true)
                parcial.delete()
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun nombreDeArchivo(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (i >= 0) c.getString(i)?.let { return it }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "archivo"
    }

    // ---------- Avance y evaluaciones (estudiante) ----------

    suspend fun miProgreso(): Progreso = withContext(Dispatchers.IO) {
        JSONObject(call("GET", "/api/progreso/mio")).toProgreso()
    }

    /** Marca un material como visto; devuelve el avance actualizado. */
    suspend fun completarMaterial(id: Long): Progreso = withContext(Dispatchers.IO) {
        JSONObject(call("POST", "/api/progreso/recursos/$id", JSONObject())).toProgreso()
    }

    suspend fun preguntas(unidadId: Long): List<PreguntaApi> = withContext(Dispatchers.IO) {
        val arr = JSONArray(call("GET", "/api/evaluaciones/$unidadId/preguntas"))
        List(arr.length()) {
            val o = arr.getJSONObject(it)
            val ops = o.getJSONArray("opciones")
            PreguntaApi(
                id = o.getLong("id"),
                unidadId = o.getLong("unidadId"),
                enunciado = o.getString("enunciado"),
                opciones = List(ops.length()) { i -> ops.getString(i) }
            )
        }
    }

    /** Envía las respuestas (preguntaId -> opción elegida) y devuelve el puntaje. */
    suspend fun responderEvaluacion(unidadId: Long, respuestas: Map<Long, Int>): ResultadoApi =
        withContext(Dispatchers.IO) {
            val arr = JSONArray()
            respuestas.forEach { (preguntaId, opcion) ->
                arr.put(JSONObject().put("preguntaId", preguntaId).put("opcion", opcion))
            }
            val o = JSONObject(call("POST", "/api/evaluaciones/$unidadId/responder", JSONObject().put("respuestas", arr)))
            val det = o.getJSONArray("detalle")
            ResultadoApi(
                unidadId = o.getLong("unidadId"),
                puntaje = o.getInt("puntaje"),
                total = o.getInt("total"),
                porcentaje = o.getInt("porcentaje"),
                detalle = (0 until det.length()).associate {
                    val d = det.getJSONObject(it)
                    d.getLong("preguntaId") to (d.getBoolean("correcta") to d.getInt("opcionCorrecta"))
                }
            )
        }

    // ---------- Conversión de JSON a modelos ----------

    private fun JSONObject.toProgreso(): Progreso {
        val ids = getJSONArray("recursosCompletadosIds")
        return Progreso(
            totalRecursos = getInt("totalRecursos"),
            recursosCompletados = getInt("recursosCompletados"),
            recursosCompletadosIds = (0 until ids.length()).map { ids.getLong(it) }.toSet(),
            totalEvaluaciones = getInt("totalEvaluaciones"),
            evaluacionesRendidas = getInt("evaluacionesRendidas"),
            porcentaje = getInt("porcentaje")
        )
    }

    // El id numérico del servidor se expone en la app como "u<id>" (u1, u2...), igual que los datos de ejemplo locales.
    private fun JSONObject.toUnidad() = UnidadPedagogica(
        id = "u${getLong("id")}",
        asignatura = AsignaturaUTP.valueOf(getString("asignatura")),
        numero = getInt("numero"),
        titulo = getString("titulo"),
        descripcion = getString("descripcion"),
        estado = EstadoContenido.valueOf(getString("estado")),
        observacionesUTP = if (isNull("observacionesUTP")) null else getString("observacionesUTP"),
        modificadoPor = getString("modificadoPor"),
        timestamp = getLong("timestamp")
    )

    private fun JSONObject.toHistorial() = HistorialAuditoria(
        id = getLong("id").toString(),
        entidadId = "u${getLong("entidadId")}",
        accion = getString("accion"),
        usuario = getString("usuario"),
        detalle = getString("detalle"),
        timestamp = getLong("timestamp")
    )
}
