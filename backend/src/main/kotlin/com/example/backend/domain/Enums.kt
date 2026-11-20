package com.example.backend.domain

enum class Rol { ALUMNO, DOCENTE, JEFATURA }

// Mismos estados que EstadoContenido en la app Android
enum class EstadoContenido {
    BORRADOR,
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    PUBLICADO,
    DESHABILITADO
}

// Mismas asignaturas que AsignaturaUTP en la app Android
enum class AsignaturaUTP(val displayName: String) {
    LENGUAJE("Lenguaje"),
    MATEMATICAS("Matemáticas"),
    CIENCIAS("Ciencias Naturales"),
    HISTORIA("Historia")
}
