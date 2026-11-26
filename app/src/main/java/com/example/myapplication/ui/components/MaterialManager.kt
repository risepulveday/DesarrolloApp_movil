package com.example.myapplication.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import com.example.myapplication.data.model.AsignaturaUTP
import com.example.myapplication.data.model.Material
import com.example.myapplication.data.remote.ApiClient
import com.example.myapplication.data.remote.ApiException
import com.example.myapplication.ui.theme.*
import kotlinx.coroutines.launch

/** Convierte un error de red en un mensaje entendible para el usuario. */
fun mensajeDeError(e: Exception): String = when {
    e is ApiException && e.code == 403 -> "No tienes permiso para esta acción."
    e is ApiException && e.code == 401 -> "Tu sesión venció. Ingresa nuevamente."
    e is ApiException -> "El servidor rechazó la solicitud (${e.code})."
    else -> "No se pudo conectar con el servidor. Verifica que esté encendido."
}

fun textoTamano(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
    bytes >= 1024 -> "${bytes / 1024} KB"
    else -> "$bytes B"
}

/**
 * Panel de material pedagógico para Docente y Jefatura:
 *  - Subir un archivo (PDF, imagen, video...) con el selector de archivos del teléfono.
 *  - Ver el material con su estado (pendiente, autorizado, rechazado).
 *  - Solo Jefatura: aprobar o rechazar el material pendiente.
 *
 * El material que sube un docente queda PENDIENTE hasta que Jefatura lo autorice;
 * el que sube Jefatura queda autorizado de inmediato. Solo el material autorizado lo ven los estudiantes.
 */
@Composable
fun MaterialManager(rol: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val esJefatura = rol.uppercase() == "JEFATURA"

    var materiales by remember { mutableStateOf<List<Material>>(emptyList()) }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var archivoElegido by remember { mutableStateOf<Uri?>(null) }
    var materialARechazar by remember { mutableStateOf<Material?>(null) }

    fun recargar() {
        scope.launch {
            cargando = true
            error = null
            try {
                materiales = ApiClient.listarMaterial()
            } catch (e: Exception) {
                error = mensajeDeError(e)
            } finally {
                cargando = false
            }
        }
    }

    LaunchedEffect(Unit) { recargar() }

    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) archivoElegido = uri
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = UtpCardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Material pedagógico", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                    Text(
                        if (esJefatura) "Autoriza el material de los docentes o sube el tuyo"
                        else "Sube archivos; Jefatura UTP debe autorizarlos antes de que los vean los estudiantes",
                        fontSize = 11.sp,
                        color = UtpTextSecondary
                    )
                }
                IconButton(onClick = { recargar() }, enabled = !cargando) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = UtpPurplePrimary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = { selector.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UtpPurplePrimary)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Subir material", fontWeight = FontWeight.Bold)
            }

            if (cargando) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = UtpPurplePrimary)
            }

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = UtpBadgeRedText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (!cargando && error == null && materiales.isEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Aún no hay material subido.", fontSize = 12.sp, color = UtpTextSecondary)
            }

            materiales.forEach { m ->
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = UtpTextSecondary.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))
                FilaMaterial(
                    material = m,
                    esJefatura = esJefatura,
                    onAprobar = {
                        scope.launch {
                            try {
                                ApiClient.cambiarEstadoMaterial(m.id, "APROBADO", null)
                                Toast.makeText(context, "Material autorizado", Toast.LENGTH_SHORT).show()
                                recargar()
                            } catch (e: Exception) {
                                Toast.makeText(context, mensajeDeError(e), Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onRechazar = { materialARechazar = m }
                )
            }
        }
    }

    // Formulario de subida (aparece después de elegir el archivo)
    archivoElegido?.let { uri ->
        DialogoSubirMaterial(
            uri = uri,
            alCancelar = { archivoElegido = null },
            alSubido = {
                archivoElegido = null
                recargar()
            }
        )
    }

    // Motivo del rechazo
    materialARechazar?.let { m ->
        var motivo by remember(m.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { materialARechazar = null },
            title = { Text("Rechazar material") },
            text = {
                Column {
                    Text(m.titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        label = { Text("Observaciones para el docente") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val observaciones = motivo.ifBlank { null }
                    materialARechazar = null
                    scope.launch {
                        try {
                            ApiClient.cambiarEstadoMaterial(m.id, "RECHAZADO", observaciones)
                            recargar()
                        } catch (e: Exception) {
                            Toast.makeText(context, mensajeDeError(e), Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Rechazar", color = UtpBadgeRedText, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { materialARechazar = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun FilaMaterial(
    material: Material,
    esJefatura: Boolean,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    val (fondo, texto, etiqueta) = when (material.estado) {
        "PENDIENTE" -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "Pendiente")
        "RECHAZADO" -> Triple(UtpBadgeRedBg, UtpBadgeRedText, "Rechazado")
        "APROBADO", "PUBLICADO" -> Triple(UtpBadgeGreenBg, UtpBadgeGreenText, "Autorizado")
        else -> Triple(UtpPurpleSoft, UtpPurplePrimary, material.estado)
    }
    val icono = when (material.tipo) {
        "PDF" -> Icons.Default.PictureAsPdf
        "IMAGEN" -> Icons.Default.Image
        "VIDEO" -> Icons.Default.VideoFile
        else -> Icons.Default.InsertDriveFile
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, tint = UtpPurplePrimary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(material.titulo, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                Text(
                    "${material.nombreArchivo} • ${textoTamano(material.tamano)} • ${material.subidoPorNombre}",
                    fontSize = 10.sp,
                    color = UtpTextSecondary
                )
            }
            Surface(shape = RoundedCornerShape(6.dp), color = fondo) {
                Text(
                    etiqueta,
                    color = texto,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        if (material.estado == "RECHAZADO" && !material.observaciones.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Observación: ${material.observaciones}", fontSize = 11.sp, color = UtpBadgeRedText)
        }

        // Solo Jefatura autoriza o rechaza
        if (esJefatura && material.estado != "APROBADO" && material.estado != "PUBLICADO") {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAprobar,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UtpBadgeGreenText),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) { Text("Autorizar", fontSize = 12.sp, fontWeight = FontWeight.Bold) }

                if (material.estado != "RECHAZADO") {
                    OutlinedButton(
                        onClick = onRechazar,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) { Text("Rechazar", fontSize = 12.sp, color = UtpBadgeRedText, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun DialogoSubirMaterial(uri: Uri, alCancelar: () -> Unit, alSubido: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var asignatura by remember { mutableStateOf<AsignaturaUTP?>(null) }
    var subiendo by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!subiendo) alCancelar() },
        title = { Text("Subir material") },
        text = {
            Column {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = { titulo = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Asignatura", fontSize = 11.sp, color = UtpTextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AsignaturaUTP.values().forEach { a ->
                        FilterChip(
                            selected = asignatura == a,
                            onClick = { asignatura = if (asignatura == a) null else a },
                            label = { Text(a.displayName.take(8), fontSize = 10.sp) }
                        )
                    }
                }
                if (subiendo) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = UtpBadgeRedText, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !subiendo && titulo.isNotBlank(),
                onClick = {
                    subiendo = true
                    error = null
                    scope.launch {
                        try {
                            ApiClient.subirMaterial(context, uri, titulo.trim(), descripcion, asignatura?.name, null)
                            Toast.makeText(context, "Material subido", Toast.LENGTH_SHORT).show()
                            alSubido()
                        } catch (e: Exception) {
                            error = mensajeDeError(e)
                            subiendo = false
                        }
                    }
                }
            ) { Text("Subir", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(enabled = !subiendo, onClick = alCancelar) { Text("Cancelar") } }
    )
}
