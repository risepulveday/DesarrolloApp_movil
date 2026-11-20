package com.example.backend.service

import com.example.backend.domain.AsignaturaUTP
import com.example.backend.domain.EstadoContenido
import com.example.backend.domain.Historial
import com.example.backend.domain.HistorialRepository
import com.example.backend.domain.Unidad
import com.example.backend.domain.UnidadRepository
import com.example.backend.web.CambioEstadoRequest
import com.example.backend.web.UnidadRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class UnidadService(
    private val unidades: UnidadRepository,
    private val historial: HistorialRepository
) {

    @Transactional(readOnly = true)
    fun listar(asignatura: AsignaturaUTP?, estado: EstadoContenido?): List<Unidad> = when {
        asignatura != null && estado != null -> unidades.findByAsignaturaAndEstado(asignatura, estado)
        asignatura != null -> unidades.findByAsignatura(asignatura)
        estado != null -> unidades.findByEstado(estado)
        else -> unidades.findAll()
    }

    @Transactional(readOnly = true)
    fun obtener(id: Long): Unidad = unidades.findById(id).orElseThrow {
        ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad $id no encontrada")
    }

    @Transactional
    fun crear(req: UnidadRequest): Unidad {
        val unidad = unidades.save(
            Unidad(
                asignatura = req.asignatura!!,
                numero = req.numero,
                titulo = req.titulo,
                descripcion = req.descripcion,
                estado = req.estado,
                modificadoPor = req.modificadoPor
            )
        )
        registrar(unidad.id!!, "CREAR", req.modificadoPor, "Unidad '${unidad.titulo}' creada.")
        return unidad
    }

    // Misma lógica que UtpRepository.cambiarEstadoUnidad en la app: cualquier estado es válido y se audita
    @Transactional
    fun cambiarEstado(id: Long, req: CambioEstadoRequest): Unidad {
        val unidad = obtener(id)
        val nuevo = req.estado!!

        unidad.estado = nuevo
        unidad.observacionesUtp = req.observaciones
        unidad.timestamp = System.currentTimeMillis()

        val detalle = when (nuevo) {
            EstadoContenido.APROBADO -> "Unidad '${unidad.titulo}' APROBADA."
            EstadoContenido.RECHAZADO -> "Unidad '${unidad.titulo}' RECHAZADA. Obs: ${req.observaciones}"
            EstadoContenido.PUBLICADO -> "Unidad '${unidad.titulo}' PUBLICADA."
            EstadoContenido.DESHABILITADO -> "Unidad '${unidad.titulo}' DESHABILITADA."
            else -> "Estado de '${unidad.titulo}' actualizado a $nuevo."
        }
        registrar(id, nuevo.name, req.usuario, detalle)
        return unidad
    }

    @Transactional(readOnly = true)
    fun historial(entidadId: Long?): List<Historial> =
        if (entidadId != null) historial.findByEntidadIdOrderByTimestampDesc(entidadId)
        else historial.findAllByOrderByTimestampDesc()

    private fun registrar(entidadId: Long, accion: String, usuario: String, detalle: String) {
        historial.save(Historial(entidadId = entidadId, accion = accion, usuario = usuario, detalle = detalle))
    }
}
