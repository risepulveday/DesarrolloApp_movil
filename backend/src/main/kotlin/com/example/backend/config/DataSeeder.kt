package com.example.backend.config

import com.example.backend.domain.AsignaturaUTP
import com.example.backend.domain.Clase
import com.example.backend.domain.ClaseRepository
import com.example.backend.domain.EstadoContenido
import com.example.backend.domain.Pregunta
import com.example.backend.domain.PreguntaRepository
import com.example.backend.domain.Rol
import com.example.backend.domain.Tarea
import com.example.backend.domain.TareaRepository
import com.example.backend.domain.Unidad
import com.example.backend.domain.UnidadRepository
import com.example.backend.domain.Usuario
import com.example.backend.domain.UsuarioRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * Carga los mismos datos de prueba que usa la app Android (UTPDatabaseHelper y UtpRepository).
 * Solo inserta si la tabla está vacía, así que es seguro reiniciar el servidor.
 */
@Component
class DataSeeder(
    private val usuarios: UsuarioRepository,
    private val tareas: TareaRepository,
    private val clases: ClaseRepository,
    private val unidades: UnidadRepository,
    private val preguntas: PreguntaRepository,
    private val encoder: PasswordEncoder
) : CommandLineRunner {

    override fun run(vararg args: String) {
        if (usuarios.count() == 0L) {
            val pass = encoder.encode("123456")!!
            usuarios.saveAll(
                listOf(
                    Usuario("alumno@utp.edu", pass, "Alberto Gómez", Rol.ALUMNO),
                    Usuario("docente@utp.edu", pass, "María Angélica", Rol.DOCENTE),
                    Usuario("jefatura@utp.edu", pass, "Carlos Mendoza", Rol.JEFATURA)
                )
            )
        }

        if (tareas.count() == 0L) {
            tareas.saveAll(
                listOf(
                    Tarea("Ejercicios de derivadas", "Matemáticas • Entrega hoy", "Hoy"),
                    Tarea("Resumen de reacciones", "Química • Entrega mañana", "Mañana"),
                    Tarea("Lectura de Cien años", "Literatura • Capítulo 3", "Semana")
                )
            )
        }

        if (clases.count() == 0L) {
            clases.saveAll(
                listOf(
                    Clase("09:00 - 10:30", "Matemáticas Básica", "1A • 42 estudiantes", "Sala 204"),
                    Clase("11:00 - 12:30", "Álgebra Lineal", "1B • 38 estudiantes", "Sala 307")
                )
            )
        }

        if (unidades.count() == 0L) {
            unidades.saveAll(
                listOf(
                    Unidad(
                        AsignaturaUTP.LENGUAJE, 1, "Lectura inicial de vocales",
                        "Comprensión lectora y reconocimiento fonológico para 1° Básico.",
                        EstadoContenido.PENDIENTE
                    ),
                    Unidad(
                        AsignaturaUTP.MATEMATICAS, 1, "Conteo y números del 0 al 20",
                        "Representación de cantidades y razonamiento matemático inicial.",
                        EstadoContenido.APROBADO
                    ),
                    Unidad(
                        AsignaturaUTP.CIENCIAS, 1, "Los sentidos y el cuerpo humano",
                        "Identificación de funciones básicas del cuerpo humano.",
                        EstadoContenido.PENDIENTE
                    ),
                    Unidad(
                        AsignaturaUTP.HISTORIA, 1, "Secuencia temporal y la familia",
                        "Nociones de tiempo, días de la semana y entorno cercano.",
                        EstadoContenido.BORRADOR
                    )
                )
            )
        }

        // Evaluación de ejemplo: 2 preguntas por unidad
        if (preguntas.count() == 0L) {
            unidades.findAll().forEach { u ->
                preguntas.saveAll(
                    when (u.asignatura) {
                        AsignaturaUTP.LENGUAJE -> listOf(
                            Pregunta(u.id!!, "¿Con qué vocal comienza la palabra \"árbol\"?", mutableListOf("A", "E", "O"), 0),
                            Pregunta(u.id!!, "¿Cuántas vocales tiene la palabra \"sol\"?", mutableListOf("0", "1", "2"), 1)
                        )
                        AsignaturaUTP.MATEMATICAS -> listOf(
                            Pregunta(u.id!!, "¿Cuánto es 3 + 4?", mutableListOf("6", "7", "8"), 1),
                            Pregunta(u.id!!, "¿Qué número viene después del 19?", mutableListOf("18", "20", "21"), 1)
                        )
                        AsignaturaUTP.CIENCIAS -> listOf(
                            Pregunta(u.id!!, "¿Con qué sentido percibimos los sonidos?", mutableListOf("Vista", "Oído", "Gusto"), 1),
                            Pregunta(u.id!!, "¿Qué órgano usamos para ver?", mutableListOf("Los ojos", "Las manos", "La nariz"), 0)
                        )
                        AsignaturaUTP.HISTORIA -> listOf(
                            Pregunta(u.id!!, "¿Cuántos días tiene una semana?", mutableListOf("5", "7", "10"), 1),
                            Pregunta(u.id!!, "¿Qué día viene después del lunes?", mutableListOf("Martes", "Domingo", "Viernes"), 0)
                        )
                    }
                )
            }
        }
    }
}
