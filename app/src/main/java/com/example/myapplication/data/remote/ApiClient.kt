package com.example.myapplication.data.remote

import com.example.myapplication.data.model.AsignaturaUTP
import com.example.myapplication.data.model.EstadoContenido
import com.example.myapplication.data.model.HistorialAuditoria
import com.example.myapplication.data.model.UnidadPedagogica
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
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
 * Cliente mínimo del backend Spring Boot (carpeta backend/).
 * Usa HttpURLConnection + org.json, ambos incluidos en Android, para no agregar dependencias.
 * Todas las funciones son suspend y corren en Dispatchers.IO.
 */
object ApiClient {

    private const val TIMEOUT_MS = 8000

    private fun call(method: String, path: String, body: JSONObject? = null): String {
        val conn = URL(ApiConfig.BASE_URL + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("Accept", "application/json")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw ApiException(code, text.ifBlank { "HTTP $code" })
            return text
        } finally {
            conn.disconnect()
        }
    }

    /** Devuelve el rol ("ALUMNO", "DOCENTE", "JEFATURA") o null si las credenciales son inválidas. */
    suspend fun login(email: String, password: String): String? = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().put("email", email.trim()).put("password", password.trim())
            JSONObject(call("POST", "/api/auth/login", body)).getString("rol")
        } catch (e: ApiException) {
            if (e.code == 401) null else throw e
        }
    }

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
