package com.example.backend.config

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain
import javax.crypto.spec.SecretKeySpec

@Configuration
class AppConfig(
    @Value("\${app.jwt.secret}") private val jwtSecret: String
) {
    private val secretKey = SecretKeySpec(jwtSecret.toByteArray(Charsets.UTF_8), "HmacSHA256")

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun jwtEncoder(): JwtEncoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(secretKey))

    @Bean
    fun jwtDecoder(): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build()

    // El rol viaja en el claim "rol" (ALUMNO, DOCENTE, JEFATURA) y se convierte en ROLE_<rol>
    private fun jwtConverter(): JwtAuthenticationConverter {
        val authorities = JwtGrantedAuthoritiesConverter().apply {
            setAuthoritiesClaimName("rol")
            setAuthorityPrefix("ROLE_")
        }
        return JwtAuthenticationConverter().apply { setJwtGrantedAuthoritiesConverter(authorities) }
    }

    /**
     * Reglas de acceso:
     *  - Solo /api/auth/login es público.
     *  - Todo lo demás exige token válido; algunas acciones además exigen un rol.
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/auth/login").permitAll()
                    // Material: subir (docente y jefatura); aprobar o rechazar (solo jefatura)
                    .requestMatchers(HttpMethod.POST, "/api/recursos").hasAnyRole("DOCENTE", "JEFATURA")
                    .requestMatchers("/api/recursos/*/estado").hasRole("JEFATURA")
                    // Unidades: crear (docente y jefatura); cambiar estado (jefatura)
                    .requestMatchers(HttpMethod.POST, "/api/unidades").hasAnyRole("DOCENTE", "JEFATURA")
                    .requestMatchers("/api/unidades/*/estado").hasRole("JEFATURA")
                    .requestMatchers(HttpMethod.POST, "/api/unidades/*/preguntas").hasAnyRole("DOCENTE", "JEFATURA")
                    // Avance y evaluaciones: solo estudiantes
                    .requestMatchers("/api/progreso/mio", "/api/progreso/recursos/**").hasRole("ALUMNO")
                    .requestMatchers("/api/evaluaciones/*/responder").hasRole("ALUMNO")
                    // Avance de todos los alumnos: docente y jefatura
                    .requestMatchers("/api/progreso/alumnos").hasAnyRole("DOCENTE", "JEFATURA")
                    .requestMatchers("/api/**").authenticated()
                    .anyRequest().denyAll()
            }
            .oauth2ResourceServer { it.jwt { jwt -> jwt.jwtAuthenticationConverter(jwtConverter()) } }
        return http.build()
    }
}
