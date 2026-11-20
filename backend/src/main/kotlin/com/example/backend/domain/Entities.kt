package com.example.backend.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

// Equivale a la tabla "usuarios" de UTPDatabaseHelper (la contraseña se guarda hasheada)
@Entity
@Table(name = "usuarios")
class Usuario(
    @Column(nullable = false, unique = true)
    var email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Column(nullable = false)
    var nombre: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var rol: Rol,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
)

// Equivale a la tabla "tasks" (pantalla Alumno)
@Entity
@Table(name = "tareas")
class Tarea(
    @Column(nullable = false)
    var title: String,

    @Column(nullable = false)
    var subtitle: String,

    @Column(nullable = false)
    var badge: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
)

// Equivale a la tabla "teacher_classes" (pantalla Docente)
@Entity
@Table(name = "clases")
class Clase(
    @Column(nullable = false)
    var time: String,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var students: String,

    @Column(nullable = false)
    var room: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
)

// Equivale a UnidadPedagogica del modelo de la app
@Entity
@Table(name = "unidades")
class Unidad(
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var asignatura: AsignaturaUTP,

    @Column(nullable = false)
    var numero: Int,

    @Column(nullable = false)
    var titulo: String,

    @Column(nullable = false, length = 1000)
    var descripcion: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var estado: EstadoContenido = EstadoContenido.PENDIENTE,

    @Column(length = 1000)
    var observacionesUtp: String? = null,

    @Column(nullable = false)
    var modificadoPor: String = "Docente 1° Básico",

    @Column(nullable = false)
    var timestamp: Long = System.currentTimeMillis(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
)

// Equivale a HistorialAuditoria
@Entity
@Table(name = "historial")
class Historial(
    @Column(nullable = false)
    var entidadId: Long,

    @Column(nullable = false)
    var accion: String,

    @Column(nullable = false)
    var usuario: String,

    @Column(nullable = false, length = 1000)
    var detalle: String,

    @Column(nullable = false)
    var timestamp: Long = System.currentTimeMillis(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
)
