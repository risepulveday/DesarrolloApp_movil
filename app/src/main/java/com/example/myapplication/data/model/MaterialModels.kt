package com.example.myapplication.data.model

import org.json.JSONObject

/** Material pedagógico (PDF, imagen, video...) tal como lo entrega el backend. */
data class Material(
    val id: Long,
    val titulo: String,
    val descripcion: String?,
    val asignatura: String?,
    val unidadId: Long?,
    val tipo: String, // PDF, IMAGEN, VIDEO o ARCHIVO
    val nombreArchivo: String,
    val contentType: String,
    val tamano: Long,
    val estado: String, // PENDIENTE, APROBADO, RECHAZADO, PUBLICADO...
    val observaciones: String?,
    val subidoPorNombre: String,
    val timestamp: Long
) {
    val autorizado: Boolean get() = estado == "APROBADO" || estado == "PUBLICADO"

    fun toJson(): JSONObject = JSONObject()
        .put("id", id).put("titulo", titulo).put("descripcion", descripcion ?: JSONObject.NULL)
        .put("asignatura", asignatura ?: JSONObject.NULL)
        .put("unidadId", unidadId ?: JSONObject.NULL)
        .put("tipo", tipo).put("nombreArchivo", nombreArchivo).put("contentType", contentType)
        .put("tamano", tamano).put("estado", estado)
        .put("observaciones", observaciones ?: JSONObject.NULL)
        .put("subidoPorNombre", subidoPorNombre).put("timestamp", timestamp)

    companion object {
        private fun JSONObject.textoONulo(k: String): String? = if (isNull(k)) null else getString(k)

        // Sirve tanto para la respuesta del servidor como para el índice local de descargas
        fun fromJson(o: JSONObject) = Material(
            id = o.getLong("id"),
            titulo = o.getString("titulo"),
            descripcion = o.textoONulo("descripcion"),
            asignatura = o.textoONulo("asignatura"),
            unidadId = if (o.isNull("unidadId")) null else o.getLong("unidadId"),
            tipo = o.getString("tipo"),
            nombreArchivo = o.getString("nombreArchivo"),
            contentType = o.getString("contentType"),
            tamano = o.getLong("tamano"),
            estado = o.getString("estado"),
            observaciones = o.textoONulo("observaciones"),
            subidoPorNombre = o.getString("subidoPorNombre"),
            timestamp = o.getLong("timestamp")
        )
    }
}

/** Avance del estudiante (calculado por el servidor). */
data class Progreso(
    val totalRecursos: Int,
    val recursosCompletados: Int,
    val recursosCompletadosIds: Set<Long>,
    val totalEvaluaciones: Int,
    val evaluacionesRendidas: Int,
    val porcentaje: Int
)

/** Pregunta de la evaluación (el servidor NO envía la respuesta correcta). */
data class PreguntaApi(val id: Long, val unidadId: Long, val enunciado: String, val opciones: List<String>)

data class ResultadoApi(
    val unidadId: Long,
    val puntaje: Int,
    val total: Int,
    val porcentaje: Int,
    // preguntaId -> (acertó, índice de la opción correcta)
    val detalle: Map<Long, Pair<Boolean, Int>>
)
