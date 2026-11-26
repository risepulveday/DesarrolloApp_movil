package com.example.backend.web

import com.example.backend.domain.AsignaturaUTP
import com.example.backend.domain.ClaseRepository
import com.example.backend.domain.EstadoContenido
import com.example.backend.domain.TareaRepository
import com.example.backend.domain.UsuarioRepository
import com.example.backend.service.TokenService
import com.example.backend.service.UnidadService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val usuarios: UsuarioRepository,
    private val encoder: PasswordEncoder,
    private val tokens: TokenService
) {
    // Equivale a UTPDatabaseHelper.validarUsuario, pero devuelve el usuario completo
    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): LoginResponse {
        val usuario = usuarios.findByEmailIgnoreCase(req.email.trim())
        if (usuario == null || !encoder.matches(req.password.trim(), usuario.passwordHash)) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas")
        }
        val emitido = tokens.emitir(usuario)
        return LoginResponse(
            usuario.id!!, usuario.nombre, usuario.email, usuario.rol,
            emitido.token, emitido.expiraEn.toEpochMilli()
        )
    }
}

@RestController
@RequestMapping("/api")
class PanelController(
    private val tareas: TareaRepository,
    private val clases: ClaseRepository
) {
    @GetMapping("/tareas")
    fun tareas(): List<TareaDto> = tareas.findAll().map { it.toDto() }

    @GetMapping("/clases")
    fun clases(): List<ClaseDto> = clases.findAll().map { it.toDto() }
}

@RestController
@RequestMapping("/api")
class UnidadController(private val service: UnidadService) {

    @GetMapping("/unidades")
    fun listar(
        @RequestParam(required = false) asignatura: AsignaturaUTP?,
        @RequestParam(required = false) estado: EstadoContenido?
    ): List<UnidadDto> = service.listar(asignatura, estado).map { it.toDto() }

    @GetMapping("/unidades/{id}")
    fun obtener(@PathVariable id: Long): UnidadDto = service.obtener(id).toDto()

    @PostMapping("/unidades")
    @ResponseStatus(HttpStatus.CREATED)
    fun crear(@Valid @RequestBody req: UnidadRequest): UnidadDto = service.crear(req).toDto()

    // PUT además de PATCH: HttpURLConnection de Android no soporta PATCH
    @RequestMapping("/unidades/{id}/estado", method = [RequestMethod.PATCH, RequestMethod.PUT])
    fun cambiarEstado(
        @PathVariable id: Long,
        @Valid @RequestBody req: CambioEstadoRequest
    ): UnidadDto = service.cambiarEstado(id, req).toDto()

    @GetMapping("/historial")
    fun historial(@RequestParam(required = false) entidadId: Long?): List<HistorialDto> =
        service.historial(entidadId).map { it.toDto() }
}
