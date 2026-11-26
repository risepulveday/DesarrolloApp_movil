package com.example.backend.web

import com.example.backend.domain.AsignaturaUTP
import com.example.backend.domain.Clase
import com.example.backend.domain.EstadoContenido
import com.example.backend.domain.Historial
import com.example.backend.domain.Rol
import com.example.backend.domain.Tarea
import com.example.backend.domain.Unidad
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

// --- Auth ---
data class LoginRequest(
    @field:NotBlank val email: String,
    @field:NotBlank val password: String
)

data class LoginResponse(
    val id: Long,
    val nombre: String,
    val email: String,
    val rol: Rol,
    val token: String,
    val expiraEn: Long // milisegundos desde 1970 (epoch)
)

// --- Tareas y clases (mismos campos que TaskModel y ClassModel de la app) ---
data class TareaDto(val id: Long, val title: String, val subtitle: String, val badge: String)

data class ClaseDto(val id: Long, val time: String, val name: String, val students: String, val room: String)

fun Tarea.toDto() = TareaDto(id!!, title, subtitle, badge)
fun Clase.toDto() = ClaseDto(id!!, time, name, students, room)

// --- Unidades pedagógicas ---
data class UnidadDto(
    val id: Long,
    val asignatura: AsignaturaUTP,
    val asignaturaNombre: String,
    val numero: Int,
    val titulo: String,
    val descripcion: String,
    val estado: EstadoContenido,
    val observacionesUTP: String?,
    val modificadoPor: String,
    val timestamp: Long
)

fun Unidad.toDto() = UnidadDto(
    id = id!!,
    asignatura = asignatura,
    asignaturaNombre = asignatura.displayName,
    numero = numero,
    titulo = titulo,
    descripcion = descripcion,
    estado = estado,
    observacionesUTP = observacionesUtp,
    modificadoPor = modificadoPor,
    timestamp = timestamp
)

data class UnidadRequest(
    @field:NotNull val asignatura: AsignaturaUTP?,
    @field:Min(1) val numero: Int,
    @field:NotBlank val titulo: String,
    @field:NotBlank val descripcion: String,
    val estado: EstadoContenido = EstadoContenido.PENDIENTE,
    val modificadoPor: String = "Docente 1° Básico"
)

data class CambioEstadoRequest(
    @field:NotNull val estado: EstadoContenido?,
    val observaciones: String? = null,
    val usuario: String = "Jefatura UTP"
)

// --- Historial / auditoría ---
data class HistorialDto(
    val id: Long,
    val entidadId: Long,
    val accion: String,
    val usuario: String,
    val detalle: String,
    val timestamp: Long
)

fun Historial.toDto() = HistorialDto(id!!, entidadId, accion, usuario, detalle, timestamp)

// --- Material (recursos) ---
data class RecursoDto(
    val id: Long,
    val titulo: String,
    val descripcion: String?,
    val asignatura: AsignaturaUTP?,
    val unidadId: Long?,
    val tipo: String, // PDF, IMAGEN, VIDEO o ARCHIVO
    val nombreArchivo: String,
    val contentType: String,
    val tamano: Long,
    val estado: EstadoContenido,
    val observaciones: String?,
    val subidoPorId: Long,
    val subidoPorNombre: String,
    val timestamp: Long
)

fun com.example.backend.domain.Recurso.toDto() = RecursoDto(
    id = id!!,
    titulo = titulo,
    descripcion = descripcion,
    asignatura = asignatura,
    unidadId = unidadId,
    tipo = when {
        contentType == "application/pdf" -> "PDF"
        contentType.startsWith("image/") -> "IMAGEN"
        contentType.startsWith("video/") -> "VIDEO"
        else -> "ARCHIVO"
    },
    nombreArchivo = nombreArchivo,
    contentType = contentType,
    tamano = tamano,
    estado = estado,
    observaciones = observaciones,
    subidoPorId = subidoPorId,
    subidoPorNombre = subidoPorNombre,
    timestamp = timestamp
)

data class CambioEstadoRecursoRequest(
    @field:NotNull val estado: EstadoContenido?,
    val observaciones: String? = null
)

// --- Avance del estudiante ---
data class ProgresoDto(
    val totalRecursos: Int,
    val recursosCompletados: Int,
    val recursosCompletadosIds: List<Long>,
    val totalEvaluaciones: Int,
    val evaluacionesRendidas: Int,
    val porcentaje: Int
)

data class AlumnoProgresoDto(
    val id: Long,
    val nombre: String,
    val recursosCompletados: Int,
    val evaluacionesRendidas: Int,
    val porcentaje: Int
)

// --- Evaluaciones ---
// La respuesta correcta NO se envía al estudiante antes de responder
data class PreguntaDto(val id: Long, val unidadId: Long, val enunciado: String, val opciones: List<String>)

data class PreguntaRequest(
    @field:NotBlank val enunciado: String,
    @field:jakarta.validation.constraints.Size(min = 2, max = 6) val opciones: List<String>,
    @field:Min(0) val correcta: Int
)

data class RespuestaItem(val preguntaId: Long, val opcion: Int)

data class ResponderRequest(val respuestas: List<RespuestaItem>)

data class DetalleRespuestaDto(val preguntaId: Long, val correcta: Boolean, val opcionCorrecta: Int)

data class ResultadoDto(
    val unidadId: Long,
    val puntaje: Int,
    val total: Int,
    val porcentaje: Int,
    val detalle: List<DetalleRespuestaDto>,
    val timestamp: Long
)
