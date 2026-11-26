package com.example.backend.service

import com.example.backend.domain.Usuario
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class TokenService(
    private val encoder: JwtEncoder,
    @Value("\${app.jwt.horas:12}") private val horas: Long
) {
    data class Emitido(val token: String, val expiraEn: Instant)

    fun emitir(usuario: Usuario): Emitido {
        val ahora = Instant.now()
        val expira = ahora.plus(horas, ChronoUnit.HOURS)
        val claims = JwtClaimsSet.builder()
            .issuer("utp-backend")
            .subject(usuario.email)
            .issuedAt(ahora)
            .expiresAt(expira)
            .claim("uid", usuario.id!!)
            .claim("nombre", usuario.nombre)
            .claim("rol", usuario.rol.name)
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        val token = encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
        return Emitido(token, expira)
    }
}
