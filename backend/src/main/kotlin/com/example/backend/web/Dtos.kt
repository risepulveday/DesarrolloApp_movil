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

data class LoginResponse(val id: Long, val nombre: String, val email: String, val rol: Rol)

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
