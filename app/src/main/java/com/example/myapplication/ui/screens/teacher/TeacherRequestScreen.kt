package com.example.myapplication.ui.screens.teacher

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.AsignaturaUTP
import com.example.myapplication.data.model.EstadoContenido
import com.example.myapplication.data.model.SolicitudPermiso
import com.example.myapplication.data.model.UnidadPedagogica
import com.example.myapplication.data.repository.UtpRepository
import com.example.myapplication.data.repository.TeacherRepository
import com.example.myapplication.ui.theme.*

/** Elementos que se pueden agregar DENTRO de una unidad (la unidad es lo principal). */
enum class TipoSolicitudEnum(val displayName: String, val icon: ImageVector) {
    PRUEBA("Prueba", Icons.Default.Edit),
    DESAFIO("Desafío", Icons.Default.EmojiEvents),
    FOTO("Foto", Icons.Default.CameraAlt),
    AUDIO("Audio", Icons.Default.Mic),
    ACTIVIDAD("Actividad", Icons.Default.Assignment)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherRequestScreen(
    onBack: () -> Unit = {},
    repository: TeacherRepository = remember { TeacherRepository() }
) {
    val context = LocalContext.current
    val unidades by UtpRepository.unidadesState.collectAsState()

    var cursoSeleccionado by remember { mutableStateOf("1° Año A") }

    // Paso 1: la unidad (crear una nueva o elegir una existente)
    var crearNueva by remember { mutableStateOf(true) }
    var asignaturaNueva by remember { mutableStateOf(AsignaturaUTP.MATEMATICAS) }
    var tituloText by remember { mutableStateOf("") }
    var descripcionText by remember { mutableStateOf("") }
    var unidadExistente by remember { mutableStateOf<UnidadPedagogica?>(null) }
    var filtroAsignatura by remember { mutableStateOf<AsignaturaUTP?>(null) }

    // Paso 2: lo que se agrega dentro de la unidad (varios a la vez)
    var agregados by remember { mutableStateOf(setOf<TipoSolicitudEnum>()) }
    var motivoText by remember { mutableStateOf("") }

    val unidadLista = if (crearNueva) tituloText.isNotBlank() else unidadExistente != null

    fun limpiarCampos() {
        cursoSeleccionado = "1° Año A"
        crearNueva = true
        asignaturaNueva = AsignaturaUTP.MATEMATICAS
        tituloText = ""
        descripcionText = ""
        unidadExistente = null
        filtroAsignatura = null
        agregados = emptySet()
        motivoText = ""
    }

    fun enviarSolicitud() {
        if (!unidadLista) {
            Toast.makeText(context, if (crearNueva) "Ingresa el título de la unidad" else "Selecciona una unidad", Toast.LENGTH_SHORT).show()
            return
        }

        val nombreUnidad: String
        if (crearNueva) {
            nombreUnidad = tituloText.trim()
            UtpRepository.agregarUnidad(
                UnidadPedagogica(
                    id = System.currentTimeMillis().toString(),
                    asignatura = asignaturaNueva,
                    numero = unidades.count { it.asignatura == asignaturaNueva } + 1,
                    titulo = nombreUnidad,
                    descripcion = descripcionText.ifBlank { "Unidad propuesta para $cursoSeleccionado." },
                    estado = EstadoContenido.PENDIENTE,
                    modificadoPor = "Docente"
                )
            )
        } else {
            nombreUnidad = unidadExistente!!.titulo
        }

        val extras = agregados.joinToString(", ") { it.displayName }
        val tipo = when {
            crearNueva && extras.isEmpty() -> "Unidad nueva"
            crearNueva -> "Unidad nueva + $extras"
            extras.isEmpty() -> "Unidad"
            else -> "Agregar a unidad: $extras"
        }

        repository.agregarSolicitud(
            SolicitudPermiso(
                id = "sol_${System.currentTimeMillis()}",
                titulo = "$nombreUnidad ($cursoSeleccionado)",
                tipoPermiso = tipo,
                fechaSolicitud = "30 Oct 2026",
                estado = "PENDIENTE",
                motivoDocente = motivoText.ifBlank { "Solicitud para $cursoSeleccionado: $tipo." },
                respuestaUtp = "Solicitud ingresada. En revisión por Jefatura UTP."
            )
        )
        Toast.makeText(context, "Solicitud enviada a Jefatura UTP", Toast.LENGTH_SHORT).show()
        onBack()
    }

    Box(modifier = Modifier.fillMaxSize().background(UtpWarmBackground)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(color = Color.White, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { limpiarCampos() }) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Limpiar", color = Color.Gray, fontSize = 14.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Nueva Solicitud", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = UtpTextPrimary)
                        Text("Solicitud a UTP", fontSize = 11.sp, color = UtpPurplePrimary)
                    }
                    Button(
                        onClick = { enviarSolicitud() },
                        colors = ButtonDefaults.buttonColors(containerColor = UtpPurplePrimary),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Solicitar", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // PASO 1: UNIDAD (lo principal)
                item {
                    TarjetaPaso("1", "Unidad *", "Crea una o elige una") {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OpcionModo("Crear unidad", Icons.Default.AddCircleOutline, crearNueva, Modifier.weight(1f)) { crearNueva = true }
                            OpcionModo("Elegir existente", Icons.Default.FolderOpen, !crearNueva, Modifier.weight(1f)) { crearNueva = false }
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        if (crearNueva) {
                            Text("Asignatura *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            ChipsAsignatura(selected = asignaturaNueva, onSelect = { asignaturaNueva = it })
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Título de la unidad *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = tituloText,
                                onValueChange = { tituloText = it },
                                placeholder = { Text("Ej: Contamos del 1 al 10", fontSize = 12.sp, color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Descripción", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = descripcionText,
                                onValueChange = { descripcionText = it },
                                placeholder = { Text("¿De qué trata la unidad?", fontSize = 12.sp, color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth().height(90.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        } else {
                            ChipsAsignatura(selected = filtroAsignatura, onSelect = { filtroAsignatura = if (filtroAsignatura == it) null else it })
                            Spacer(modifier = Modifier.height(10.dp))
                            val lista = unidades.filter {
                                it.estado != EstadoContenido.RECHAZADO && (filtroAsignatura == null || it.asignatura == filtroAsignatura)
                            }
                            if (lista.isEmpty()) {
                                Text("No hay unidades para esta asignatura. Crea una nueva.", fontSize = 12.sp, color = UtpTextSecondary)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                lista.forEach { u ->
                                    val sel = unidadExistente?.id == u.id
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clickable { unidadExistente = u },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (sel) Color(0xFFF0F0FF) else Color(0xFFF7F7FA),
                                        border = if (sel) BorderStroke(1.5.dp, UtpPurplePrimary) else null
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(u.titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                                                Text("${u.asignatura.displayName} • Unidad ${u.numero}", fontSize = 11.sp, color = UtpTextSecondary)
                                            }
                                            if (sel) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = UtpPurplePrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // PASO 2 y 3: solo aparecen cuando ya hay una unidad
                if (unidadLista) {
                    item {
                        TarjetaPaso("2", "Agregar a la unidad", "Opcional, elige varios") {
                            val tipos = TipoSolicitudEnum.values()
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                tipos.toList().chunked(3).forEach { fila ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        fila.forEach { t ->
                                            ResourceTypeChoiceTeacher(
                                                tipo = t,
                                                selected = t in agregados,
                                                onClick = { agregados = if (t in agregados) agregados - t else agregados + t },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        repeat(3 - fila.size) { Spacer(modifier = Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        TarjetaPaso("3", "Curso y motivo", null) {
                            Text("Curso destino *", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                listOf("1° Año A", "1° Año B").forEach { curso ->
                                    val selected = cursoSeleccionado == curso
                                    Surface(
                                        modifier = Modifier.weight(1f).height(44.dp).clickable { cursoSeleccionado = curso },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) Color(0xFFF0F0FF) else Color(0xFFF7F7FA),
                                        border = if (selected) BorderStroke(1.5.dp, UtpPurplePrimary) else null
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selected) UtpPurplePrimary else Color.Gray)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(curso, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) UtpPurplePrimary else Color.Gray, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Motivo pedagógico", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = motivoText,
                                onValueChange = { motivoText = it },
                                placeholder = { Text("Escribe el motivo o detalle de tu solicitud...", fontSize = 12.sp, color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth().height(90.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            "Define la unidad para poder agregarle pruebas, desafíos, fotos, audios o actividades.",
                            fontSize = 12.sp,
                            color = UtpTextSecondary
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun TarjetaPaso(numero: String, titulo: String, ayuda: String?, contenido: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BadgeNumber(number = numero)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = UtpTextPrimary)
                }
                if (ayuda != null) Text(ayuda, fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            contenido()
        }
    }
}

@Composable
private fun OpcionModo(texto: String, icono: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(48.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFFF0F0FF) else Color(0xFFF7F7FA),
        border = if (selected) BorderStroke(1.5.dp, UtpPurplePrimary) else null
    ) {
        Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selected) UtpPurplePrimary else Color.Gray)
            Spacer(modifier = Modifier.width(6.dp))
            Text(texto, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) UtpPurplePrimary else Color.Gray)
        }
    }
}

@Composable
private fun ChipsAsignatura(selected: AsignaturaUTP?, onSelect: (AsignaturaUTP) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AsignaturaUTP.values().toList().chunked(2).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { a ->
                    val sel = a == selected
                    Surface(
                        modifier = Modifier.weight(1f).height(38.dp).clickable { onSelect(a) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (sel) Color(0xFFF0F0FF) else Color(0xFFF7F7FA),
                        border = if (sel) BorderStroke(1.5.dp, UtpPurplePrimary) else null
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(a.displayName, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) UtpPurplePrimary else Color.Gray)
                        }
                    }
                }
                repeat(2 - fila.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun BadgeNumber(number: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFEFEFFE),
        modifier = Modifier.size(22.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(number, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = UtpPurplePrimary)
        }
    }
}

@Composable
private fun ResourceTypeChoiceTeacher(
    tipo: TipoSolicitudEnum,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(60.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Color(0xFFF0F0FF) else Color(0xFFF7F7FA),
        border = if (selected) BorderStroke(1.5.dp, UtpPurplePrimary) else null
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                tipo.icon,
                contentDescription = null,
                tint = if (selected) UtpPurplePrimary else Color.Gray,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                tipo.displayName,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) UtpPurplePrimary else Color.Gray
            )
        }
    }
}