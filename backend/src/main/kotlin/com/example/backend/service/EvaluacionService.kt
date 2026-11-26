package com.example.backend.service

import com.example.backend.domain.Pregunta
import com.example.backend.domain.PreguntaRepository
import com.example.backend.domain.ProgresoRecurso
import com.example.backend.domain.ProgresoRecursoRepository
import com.example.backend.domain.RecursoRepository
import com.example.backend.domain.Resultado
import com.example.backend.domain.ResultadoRepository
import com.example.backend.domain.Rol
import com.example.backend.domain.UnidadRepository
import com.example.backend.domain.UsuarioRepository
import com.example.backend.web.AlumnoProgresoDto
import com.example.backend.web.DetalleRespuestaDto
import com.example.backend.web.PreguntaRequest
import com.example.backend.web.ProgresoDto
import com.example.backend.web.ResponderRequest
import com.example.backend.web.ResultadoDto
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

@Service
class EvaluacionService(
    private val preguntas: PreguntaRepository,
    private val resultados: ResultadoRepository,
    private val progreso: ProgresoRecursoRepository,
    private val recursos: RecursoRepository,
    private val unidades: UnidadRepository,
    private val usuarios: UsuarioRepository
) {

    // ---------- Preguntas ----------

    @Transactional(readOnly = true)
    fun preguntas(unidadId: Long): List<Pregunta> = preguntas.findByUnidadIdOrderById(unidadId)

    @Transactional
    fun crearPregunta(unidadId: Long, req: PreguntaRequest): Pregunta {
        if (!unidades.existsById(unidadId)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Unidad $unidadId no encontrada")
        }
        if (req.correcta !in req.opciones.indices) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La opción correcta no existe")
        }
        return preguntas.save(
            Pregunta(unidadId = unidadId, enunciado = req.enunciado.trim(), opciones = req.opciones.toMutableList(), correcta = req.correcta)
        )
    }

    // ---------- Responder ----------

    @Transactional
    fun responder(actor: Actor, unidadId: Long, req: ResponderRequest): ResultadoDto {
        val banco = preguntas.findByUnidadIdOrderById(unidadId)
        if (banco.isEmpty()) throw ResponseStatusException(HttpStatus.NOT_FOUND, "Esta unidad no tiene evaluación")

        val porId = req.respuestas.associateBy { it.preguntaId }
        val detalle = banco.map { p ->
            val elegida = porId[p.id]?.opcion
            DetalleRespuestaDto(p.id!!, elegida == p.correcta, p.correcta)
        }
        val puntaje = detalle.count { it.correcta }
        val guardado = resultados.save(Resultado(usuarioId = actor.id, unidadId = unidadId, puntaje = puntaje, total = banco.size))
        return ResultadoDto(unidadId, puntaje, banco.size, puntaje * 100 / banco.size, detalle, guardado.timestamp)
    }

    @Transactional(readOnly = true)
    fun misResultados(actor: Actor): List<ResultadoDto> =
        resultados.findByUsuarioIdOrderByTimestampDesc(actor.id).map {
            ResultadoDto(it.unidadId, it.puntaje, it.total, if (it.total == 0) 0 else it.puntaje * 100 / it.total, emptyList(), it.timestamp)
        }

    // ---------- Avance ----------

    /** Marca un recurso como completado (si ya estaba marcado no hace nada). */
    @Transactional
    fun completarRecurso(actor: Actor, recursoId: Long): ProgresoDto {
        val recurso = recursos.findById(recursoId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso $recursoId no encontrado")
        }
        if (recurso.estado !in ESTADOS_DISPONIBLES) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Este material aún no está autorizado")
        }
        if (!progreso.existsByUsuarioIdAndRecursoId(actor.id, recursoId)) {
            progreso.save(ProgresoRecurso(usuarioId = actor.id, recursoId = recursoId))
        }
        return calcular(actor.id)
    }

    @Transactional(readOnly = true)
    fun miProgreso(actor: Actor): ProgresoDto = calcular(actor.id)

    @Transactional(readOnly = true)
    fun progresoAlumnos(): List<AlumnoProgresoDto> =
        usuarios.findByRol(Rol.ALUMNO).map {
            val p = calcular(it.id!!)
            AlumnoProgresoDto(it.id!!, it.nombre, p.recursosCompletados, p.evaluacionesRendidas, p.porcentaje)
        }

    /**
     * Avance = (recursos completados + evaluaciones rendidas) / (recursos autorizados + unidades con evaluación)
     */
    private fun calcular(usuarioId: Long): ProgresoDto {
        val disponibles = recursos.findByEstadoInOrderByTimestampDesc(ESTADOS_DISPONIBLES).map { it.id!! }.toSet()
        val completados = progreso.findByUsuarioId(usuarioId).map { it.recursoId }.filter { it in disponibles }.distinct()

        val unidadesConEvaluacion = preguntas.findAll().map { it.unidadId }.toSet()
        val rendidas = resultados.findByUsuarioIdOrderByTimestampDesc(usuarioId)
            .map { it.unidadId }.filter { it in unidadesConEvaluacion }.toSet()

        val total = disponibles.size + unidadesConEvaluacion.size
        val hechos = completados.size + rendidas.size
        val porcentaje = if (total == 0) 0 else hechos * 100 / total

        return ProgresoDto(
            totalRecursos = disponibles.size,
            recursosCompletados = completados.size,
            recursosCompletadosIds = completados,
            totalEvaluaciones = unidadesConEvaluacion.size,
            evaluacionesRendidas = rendidas.size,
            porcentaje = porcentaje
        )
    }
}
