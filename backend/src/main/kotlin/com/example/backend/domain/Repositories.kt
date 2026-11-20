package com.example.backend.domain

import org.springframework.data.jpa.repository.JpaRepository

interface UsuarioRepository : JpaRepository<Usuario, Long> {
    fun findByEmailIgnoreCase(email: String): Usuario?
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
