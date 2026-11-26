package com.example.myapplication.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Sesión del usuario: token JWT, rol y expiración.
 *
 * - La navegación consulta [activa] (estado de Compose): si la sesión se cierra o expira,
 *   la app vuelve sola al login.
 * - Se guarda en SharedPreferences para poder abrir la app sin internet mientras el token
 *   no haya expirado (así el estudiante ve sus descargas sin conexión).
 */
object Session {
    private const val PREFS = "utp_session"
    private var prefs: SharedPreferences? = null

    var token: String? = null
        private set
    var rol: String? = null
        private set
    var nombre: String = ""
        private set
    var userId: Long = 0L
        private set
    private var expiraEn: Long = 0L

    // Estado observable por Compose
    var activa by mutableStateOf(false)
        private set

    /** Se llama una vez al abrir la app (MainActivity): recupera la sesión guardada si sigue vigente. */
    fun init(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        val t = p.getString("token", null)
        val exp = p.getLong("expiraEn", 0L)
        if (t != null && System.currentTimeMillis() < exp) {
            token = t
            rol = p.getString("rol", null)
            nombre = p.getString("nombre", "") ?: ""
            userId = p.getLong("userId", 0L)
            expiraEn = exp
            activa = rol != null
        } else {
            limpiarPrefs()
        }
    }

    fun iniciar(token: String, rol: String, nombre: String, userId: Long, expiraEn: Long) {
        this.token = token
        this.rol = rol.uppercase()
        this.nombre = nombre
        this.userId = userId
        this.expiraEn = expiraEn
        prefs?.edit()
            ?.putString("token", token)
            ?.putString("rol", this.rol)
            ?.putString("nombre", nombre)
            ?.putLong("userId", userId)
            ?.putLong("expiraEn", expiraEn)
            ?.apply()
        activa = true
    }

    /** Cierra la sesión (logout, token vencido o rechazado por el servidor). */
    fun cerrar() {
        token = null
        rol = null
        nombre = ""
        userId = 0L
        expiraEn = 0L
        limpiarPrefs()
        activa = false
    }

    /** Revisa que el token no haya vencido; si venció, cierra la sesión. */
    fun vigente(): Boolean {
        if (token != null && System.currentTimeMillis() >= expiraEn) cerrar()
        return activa
    }

    private fun limpiarPrefs() {
        prefs?.edit()?.clear()?.apply()
    }
}
