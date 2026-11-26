package com.example.backend.service

import com.example.backend.domain.AsignaturaUTP
import com.example.backend.domain.EstadoContenido
import com.example.backend.domain.Recurso
import com.example.backend.domain.RecursoRepository
import com.example.backend.domain.Rol
import com.example.backend.web.CambioEstadoRecursoRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.UUID

/** Quién hace la petición, leído del token JWT */
data class Actor(val id: Long, val nombre: String, val rol: Rol)

fun Jwt.actor() = Actor(
    id = (claims["uid"] as Number).toLong(),
    nombre = getClaimAsString("nombre"),
    rol = Rol.valueOf(getClaimAsString("rol"))
)

// Estados en que el material ya es visible para los estudiantes
val ESTADOS_DISPONIBLES = listOf(EstadoContenido.APROBADO, EstadoContenido.PUBLICADO)

@Service
class RecursoService(
    private val recursos: RecursoRepository,
    @Value("\${app.storage.dir}") storageDir: String
) {
    private val base: Path = Paths.get(storageDir).toAbsolutePath().normalize()

    @Transactional(readOnly = true)
    fun listar(actor: Actor, estado: EstadoContenido?): List<Recurso> {
        val todos = when (actor.rol) {
            Rol.JEFATURA -> if (estado != null) recursos.findByEstadoOrderByTimestampDesc(estado) else recursos.findAllByOrderByTimestampDesc()
            Rol.DOCENTE -> {
                // Un docente ve lo suyo (en cualquier estado) más lo ya autorizado de otros
                val propios = recursos.findBySubidoPorIdOrderByTimestampDesc(actor.id)
                val autorizados = recursos.findByEstadoInOrderByTimestampDesc(ESTADOS_DISPONIBLES)
                (propios + autorizados).distinctBy { it.id }.sortedByDescending { it.timestamp }
                    .filter { estado == null || it.estado == estado }
            }
            Rol.ALUMNO -> recursos.findByEstadoInOrderByTimestampDesc(ESTADOS_DISPONIBLES)
                .filter { estado == null || it.estado == estado }
        }
        return todos
    }

    @Transactional
    fun subir(
        actor: Actor,
        titulo: String,
        descripcion: String?,
        asignatura: AsignaturaUTP?,
        unidadId: Long?,
        archivo: MultipartFile
    ): Recurso {
        if (archivo.isEmpty) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío")
        if (titulo.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El título es obligatorio")

        // Solo el nombre del archivo, sin carpetas (evita rutas tipo ../../)
        val original = (archivo.originalFilename ?: "archivo").substringAfterLast('/').substringAfterLast('\\')
        val extension = original.substringAfterLast('.', "").filter { it.isLetterOrDigit() }.take(10)
        val guardado = if (extension.isEmpty()) UUID.randomUUID().toString() else "${UUID.randomUUID()}.$extension"

        Files.createDirectories(base)
        archivo.inputStream.use { Files.copy(it, base.resolve(guardado), StandardCopyOption.REPLACE_EXISTING) }

        // El material del docente queda PENDIENTE hasta que Jefatura lo autorice; el de Jefatura nace autorizado
        val estadoInicial = if (actor.rol == Rol.JEFATURA) EstadoContenido.APROBADO else EstadoContenido.PENDIENTE

        return recursos.save(
            Recurso(
                titulo = titulo.trim(),
                descripcion = descripcion?.trim()?.ifBlank { null },
                asignatura = asignatura,
                unidadId = unidadId,
                nombreArchivo = original,
                contentType = archivo.contentType ?: "application/octet-stream",
                tamano = archivo.size,
                archivoGuardado = guardado,
                subidoPorId = actor.id,
                subidoPorNombre = actor.nombre,
                estado = estadoInicial
            )
        )
    }

    @Transactional
    fun cambiarEstado(id: Long, req: CambioEstadoRecursoRequest): Recurso {
        val recurso = obtener(id)
        recurso.estado = req.estado!!
        recurso.observaciones = req.observaciones
        recurso.timestamp = System.currentTimeMillis()
        return recurso
    }

    @Transactional(readOnly = true)
    fun obtener(id: Long): Recurso = recursos.findById(id).orElseThrow {
        ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso $id no encontrado")
    }

    /** Devuelve el archivo, solo si la persona tiene permiso de verlo. */
    @Transactional(readOnly = true)
    fun archivo(id: Long, actor: Actor): Pair<Recurso, Resource> {
        val recurso = obtener(id)
        val autorizado = recurso.estado in ESTADOS_DISPONIBLES
        val puede = when (actor.rol) {
            Rol.JEFATURA -> true
            Rol.DOCENTE -> autorizado || recurso.subidoPorId == actor.id
            Rol.ALUMNO -> autorizado
        }
        if (!puede) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Este material aún no está autorizado")

        val ruta = base.resolve(recurso.archivoGuardado).normalize()
        if (!ruta.startsWith(base) || !Files.exists(ruta)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "El archivo no existe en el servidor")
        }
        return recurso to UrlResource(ruta.toUri())
    }

    @Transactional(readOnly = true)
    fun contarDisponibles(): Long = recursos.countByEstadoIn(ESTADOS_DISPONIBLES)
}
