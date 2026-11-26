package com.example.backend.domain

import org.springframework.data.jpa.repository.JpaRepository

interface UsuarioRepository : JpaRepository<Usuario, Long> {
    fun findByEmailIgnoreCase(email: String): Usuario?
    fun findByRol(rol: Rol): List<Usuario>
}

interface TareaRepository : JpaRepository<Tarea, Long>

interface ClaseRepository : JpaRepository<Clase, Long>

interface UnidadRepository : JpaRepository<Unidad, Long> {
    fun findByAsignatura(asignatura: AsignaturaUTP): List<Unidad>
    fun findByEstado(estado: EstadoContenido): List<Unidad>
    fun findByAsignaturaAndEstado(asignatura: AsignaturaUTP, estado: EstadoContenido): List<Unidad>
}

interface HistorialRepository : JpaRepository<Historial, Long> {
    fun findAllByOrderByTimestampDesc(): List<Historial>
    fun findByEntidadIdOrderByTimestampDesc(entidadId: Long): List<Historial>
}

interface RecursoRepository : JpaRepository<Recurso, Long> {
    fun findAllByOrderByTimestampDesc(): List<Recurso>
    fun findByEstadoInOrderByTimestampDesc(estados: Collection<EstadoContenido>): List<Recurso>
    fun findByEstadoOrderByTimestampDesc(estado: EstadoContenido): List<Recurso>
    fun findBySubidoPorIdOrderByTimestampDesc(subidoPorId: Long): List<Recurso>
    fun countByEstadoIn(estados: Collection<EstadoContenido>): Long
}

interface ProgresoRecursoRepository : JpaRepository<ProgresoRecurso, Long> {
    fun findByUsuarioId(usuarioId: Long): List<ProgresoRecurso>
    fun existsByUsuarioIdAndRecursoId(usuarioId: Long, recursoId: Long): Boolean
}

interface PreguntaRepository : JpaRepository<Pregunta, Long> {
    fun findByUnidadIdOrderById(unidadId: Long): List<Pregunta>
}

interface ResultadoRepository : JpaRepository<Resultado, Long> {
    fun findByUsuarioIdOrderByTimestampDesc(usuarioId: Long): List<Resultado>
}
