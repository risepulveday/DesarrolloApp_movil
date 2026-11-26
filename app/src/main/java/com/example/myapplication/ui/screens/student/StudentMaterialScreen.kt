package com.example.myapplication.ui.screens.student

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.local.DescargasLocales
import com.example.myapplication.data.model.EstadoContenido
import com.example.myapplication.data.model.Material
import com.example.myapplication.data.model.Progreso
import com.example.myapplication.data.model.UnidadPedagogica
import com.example.myapplication.data.remote.ApiClient
import com.example.myapplication.data.remote.Session
import com.example.myapplication.ui.components.UtpTopHeaderBar
import com.example.myapplication.ui.components.mensajeDeError
import com.example.myapplication.ui.components.textoTamano
import com.example.myapplication.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Contenido del estudiante:
 *  - Barra de avance real (la calcula el servidor).
 *  - Material autorizado por Jefatura UTP, que se puede DESCARGAR y abrir sin conexión a internet.
 *  - Evaluaciones de las unidades autorizadas.
 * Sin conexión muestra solo el material que ya está descargado en el teléfono.
 */
@Composable
fun StudentMaterialScreen(
    onBack: () -> Unit = {},
    onStartQuiz: (Long) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var materiales by remember { mutableStateOf<List<Material>>(emptyList()) }
    var evaluaciones by remember { mutableStateOf<List<UnidadPedagogica>>(emptyList()) }
    var progreso by remember { mutableStateOf<Progreso?>(null) }
    var sinConexion by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(true) }
    var descargando by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var refresco by remember { mutableIntStateOf(0) } // fuerza releer qué archivos están descargados

    fun cargar() {
        scope.launch {
            cargando = true
            try {
                materiales = ApiClient.listarMaterial()
                sinConexion = false
            } catch (e: Exception) {
                // Sin servidor: se muestra lo que ya está descargado en el teléfono
                sinConexion = true
                materiales = DescargasLocales.listar(context)
            }
            try {
                progreso = ApiClient.miProgreso()
            } catch (e: Exception) { /* sin conexión: se deja el último valor */ }
            try {
                evaluaciones = ApiClient.getUnidades()
                    .filter { it.estado == EstadoContenido.APROBADO || it.estado == EstadoContenido.PUBLICADO }
            } catch (e: Exception) { /* las evaluaciones requieren conexión */ }
            cargando = false
        }
    }

    LaunchedEffect(Unit) { cargar() }

    fun descargar(m: Material) {
        descargando = descargando + m.id
        scope.launch {
            try {
                DescargasLocales.descargar(context, m)
                Toast.makeText(context, "Descargado: ya puedes verlo sin internet", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, mensajeDeError(e), Toast.LENGTH_LONG).show()
            } finally {
                descargando = descargando - m.id
                refresco++
            }
        }
    }

    fun abrir(m: Material) {
        if (!DescargasLocales.abrir(context, m)) {
            Toast.makeText(context, "No hay una aplicación en tu teléfono para abrir este archivo", Toast.LENGTH_LONG).show()
            return
        }
        // Abrir un material lo cuenta como visto en la barra de avance
        scope.launch {
            try {
                progreso = ApiClient.completarMaterial(m.id)
            } catch (e: Exception) { /* sin conexión: no se registra el avance */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(UtpWarmBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            UtpTopHeaderBar(
                userRole = "ALUMNO",
                userName = Session.nombre,
                showBack = true,
                onBack = onBack,
                onLogout = onLogout
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(modifier = Modifier.height(10.dp)) }

                // BARRA DE AVANCE
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = UtpCardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val porcentaje = progreso?.porcentaje ?: 0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Mi avance", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                                Text("$porcentaje%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = UtpPurplePrimary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { porcentaje / 100f },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = UtpBadgeGreenText,
                                trackColor = UtpPurpleSoft,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            progreso?.let {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "${it.recursosCompletados} de ${it.totalRecursos} materiales vistos • " +
                                        "${it.evaluacionesRendidas} de ${it.totalEvaluaciones} evaluaciones rendidas",
                                    fontSize = 11.sp,
                                    color = UtpTextSecondary
                                )
                            }
                        }
                    }
                }

                if (sinConexion) {
                    item {
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFEF3C7)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudOff, contentDescription = null, tint = Color(0xFFD97706))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Sin conexión: se muestra el material que descargaste.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFD97706),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // MATERIAL
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Material de estudio", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                        IconButton(onClick = { cargar() }, enabled = !cargando) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = UtpPurplePrimary)
                        }
                    }
                    if (cargando) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = UtpPurplePrimary)
                }

                if (!cargando && materiales.isEmpty()) {
                    item {
                        Text(
                            if (sinConexion) "No tienes material descargado."
                            else "Todavía no hay material autorizado por Jefatura UTP.",
                            fontSize = 12.sp,
                            color = UtpTextSecondary
                        )
                    }
                }

                items(materiales, key = { it.id }) { m ->
                    val descargado = remember(refresco, m.id) { DescargasLocales.estaDescargado(context, m) }
                    val visto = progreso?.recursosCompletadosIds?.contains(m.id) == true
                    val icono = when (m.tipo) {
                        "PDF" -> Icons.Default.PictureAsPdf
                        "IMAGEN" -> Icons.Default.Image
                        "VIDEO" -> Icons.Default.VideoFile
                        else -> Icons.Default.InsertDriveFile
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = UtpCardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(icono, contentDescription = null, tint = UtpPurplePrimary, modifier = Modifier.size(30.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(m.titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                                    if (visto) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Visto", tint = UtpBadgeGreenText, modifier = Modifier.size(16.dp))
                                    }
                                }
                                Text(
                                    listOfNotNull(m.asignatura, textoTamano(m.tamano), if (descargado) "Descargado" else null).joinToString(" • "),
                                    fontSize = 10.sp,
                                    color = UtpTextSecondary
                                )
                                m.descripcion?.let { Text(it, fontSize = 11.sp, color = UtpTextSecondary) }
                            }
                            Spacer(modifier = Modifier.width(8.dp))

                            when {
                                m.id in descargando -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                descargado -> Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = { abrir(m) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = UtpPurplePrimary)
                                    ) { Text("Abrir", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                    IconButton(onClick = {
                                        DescargasLocales.eliminar(context, m)
                                        if (sinConexion) materiales = materiales.filter { it.id != m.id }
                                        refresco++
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Quitar descarga", tint = UtpTextSecondary)
                                    }
                                }
                                else -> OutlinedButton(
                                    onClick = { descargar(m) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Descargar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // EVALUACIONES
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Evaluaciones", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                }

                if (evaluaciones.isEmpty()) {
                    item {
                        Text(
                            if (sinConexion) "Las evaluaciones necesitan conexión a internet."
                            else "No hay evaluaciones disponibles por ahora.",
                            fontSize = 12.sp,
                            color = UtpTextSecondary
                        )
                    }
                }

                items(evaluaciones, key = { it.id }) { u ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = UtpCardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(u.titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                                Text("${u.asignatura.displayName} • Unidad ${u.numero}", fontSize = 10.sp, color = UtpTextSecondary)
                            }
                            Button(
                                onClick = { u.id.removePrefix("u").toLongOrNull()?.let(onStartQuiz) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = UtpRed)
                            ) { Text("Rendir", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}
