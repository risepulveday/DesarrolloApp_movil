package com.example.backend.config

import com.example.backend.domain.AsignaturaUTP
import com.example.backend.domain.Clase
import com.example.backend.domain.ClaseRepository
import com.example.backend.domain.EstadoContenido
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
    }
}
