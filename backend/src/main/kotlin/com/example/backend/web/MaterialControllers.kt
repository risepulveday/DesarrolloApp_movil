package com.example.backend.web

import com.example.backend.domain.AsignaturaUTP
import com.example.backend.domain.EstadoContenido
import com.example.backend.service.EvaluacionService
import com.example.backend.service.RecursoService
import com.example.backend.service.actor
import jakarta.validation.Valid
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets

@RestController
@RequestMapping("/api/recursos")
class RecursoController(private val service: RecursoService) {

    // Cada rol ve distinto: alumno = autorizado, docente = lo suyo + autorizado, jefatura = todo
    @GetMapping
    fun listar(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(name = "estado", required = false) estado: EstadoContenido?
    ): List<RecursoDto> = service.listar(jwt.actor(), estado).map { it.toDto() }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @ResponseStatus(HttpStatus.CREATED)
    fun subir(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam("titulo") titulo: String,
        @RequestParam("descripcion", required = false) descripcion: String?,
        @RequestParam("asignatura", required = false) asignatura: AsignaturaUTP?,
        @RequestParam("unidadId", required = false) unidadId: Long?,
        @RequestParam("archivo") archivo: MultipartFile
    ): RecursoDto = service.subir(jwt.actor(), titulo, descripcion, asignatura, unidadId, archivo).toDto()

    // PUT además de PATCH: HttpURLConnection de Android no soporta PATCH
    @RequestMapping("/{id}/estado", method = [RequestMethod.PATCH, RequestMethod.PUT])
    fun cambiarEstado(
        @PathVariable("id") id: Long,
        @Valid @RequestBody req: CambioEstadoRecursoRequest
    ): RecursoDto = service.cambiarEstado(id, req).toDto()

    // Descarga del archivo (para verlo sin conexión desde la app)
    @GetMapping("/{id}/archivo")
    fun archivo(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable("id") id: Long
    ): ResponseEntity<org.springframework.core.io.Resource> {
        val (recurso, archivo) = service.archivo(id, jwt.actor())
        val disposicion = ContentDisposition.attachment().filename(recurso.nombreArchivo, StandardCharsets.UTF_8).build()
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(recurso.contentType))
            .contentLength(recurso.tamano)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
            .body(archivo)
    }
}

@RestController
@RequestMapping("/api/progreso")
class ProgresoController(private val service: EvaluacionService) {

    @GetMapping("/mio")
    fun mio(@AuthenticationPrincipal jwt: Jwt): ProgresoDto = service.miProgreso(jwt.actor())

    // El estudiante marca un recurso como visto/completado
    @PostMapping("/recursos/{recursoId}")
    fun completar(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable("recursoId") recursoId: Long
    ): ProgresoDto = service.completarRecurso(jwt.actor(), recursoId)

    @GetMapping("/alumnos")
    fun alumnos(): List<AlumnoProgresoDto> = service.progresoAlumnos()
}

@RestController
@RequestMapping("/api")
class EvaluacionController(private val service: EvaluacionService) {

    @GetMapping("/evaluaciones/{unidadId}/preguntas")
    fun preguntas(@PathVariable("unidadId") unidadId: Long): List<PreguntaDto> =
        service.preguntas(unidadId).map { PreguntaDto(it.id!!, it.unidadId, it.enunciado, it.opciones.toList()) }

    @PostMapping("/unidades/{unidadId}/preguntas")
    @ResponseStatus(HttpStatus.CREATED)
    fun crearPregunta(
        @PathVariable("unidadId") unidadId: Long,
        @Valid @RequestBody req: PreguntaRequest
    ): PreguntaDto {
        val p = service.crearPregunta(unidadId, req)
        return PreguntaDto(p.id!!, p.unidadId, p.enunciado, p.opciones.toList())
    }

    @PostMapping("/evaluaciones/{unidadId}/responder")
    fun responder(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable("unidadId") unidadId: Long,
        @RequestBody req: ResponderRequest
    ): ResultadoDto = service.responder(jwt.actor(), unidadId, req)

    @GetMapping("/evaluaciones/mis-resultados")
    fun misResultados(@AuthenticationPrincipal jwt: Jwt): List<ResultadoDto> = service.misResultados(jwt.actor())
}
