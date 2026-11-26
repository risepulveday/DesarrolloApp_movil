package com.example.myapplication.ui.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.PreguntaApi
import com.example.myapplication.data.model.ResultadoApi
import com.example.myapplication.data.remote.ApiClient
import com.example.myapplication.data.remote.Session
import com.example.myapplication.ui.components.UtpTopHeaderBar
import com.example.myapplication.ui.components.mensajeDeError
import com.example.myapplication.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Evaluación de una unidad: las preguntas vienen del servidor y el resultado se guarda allá
 * (cuenta para la barra de avance). La respuesta correcta solo se revela después de enviar.
 */
@Composable
fun StudentEvaluacionScreen(
    unidadId: Long,
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var preguntas by remember { mutableStateOf<List<PreguntaApi>>(emptyList()) }
    var elegidas by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var resultado by remember { mutableStateOf<ResultadoApi?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(unidadId) {
        try {
            preguntas = ApiClient.preguntas(unidadId)
        } catch (e: Exception) {
            error = mensajeDeError(e)
        } finally {
            cargando = false
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

                item {
                    Text("Evaluación de la unidad", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                    if (cargando) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = UtpPurplePrimary)
                    }
                    error?.let { Text(it, color = UtpBadgeRedText, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    if (!cargando && error == null && preguntas.isEmpty()) {
                        Text("Esta unidad todavía no tiene preguntas.", fontSize = 12.sp, color = UtpTextSecondary)
                    }
                }

                // RESULTADO
                resultado?.let { r ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (r.porcentaje >= 60) UtpBadgeGreenBg else UtpBadgeRedBg
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${r.puntaje} de ${r.total} correctas", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                                Text("${r.porcentaje}%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                            }
                        }
                    }
                }

                // PREGUNTAS
                itemsIndexed(preguntas, key = { _, p -> p.id }) { indice, p ->
                    val detalle = resultado?.detalle?.get(p.id)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = UtpCardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("${indice + 1}. ${p.enunciado}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = UtpTextPrimary)
                            Spacer(modifier = Modifier.height(6.dp))

                            p.opciones.forEachIndexed { i, texto ->
                                val seleccionada = elegidas[p.id] == i
                                val color = when {
                                    detalle == null -> UtpTextPrimary
                                    i == detalle.second -> UtpBadgeGreenText // la correcta
                                    seleccionada -> UtpBadgeRedText // la que eligió y estaba mal
                                    else -> UtpTextSecondary
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = resultado == null) {
                                            elegidas = elegidas + (p.id to i)
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = seleccionada,
                                        onClick = if (resultado == null) ({ elegidas = elegidas + (p.id to i) }) else null,
                                        colors = RadioButtonDefaults.colors(selectedColor = UtpPurplePrimary)
                                    )
                                    Text(texto, fontSize = 13.sp, color = color, fontWeight = if (detalle != null && i == detalle.second) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }

                // BOTONES
                item {
                    if (resultado == null) {
                        Button(
                            onClick = {
                                enviando = true
                                error = null
                                scope.launch {
                                    try {
                                        resultado = ApiClient.responderEvaluacion(unidadId, elegidas)
                                    } catch (e: Exception) {
                                        error = mensajeDeError(e)
                                    } finally {
                                        enviando = false
                                    }
                                }
                            },
                            enabled = !enviando && preguntas.isNotEmpty() && elegidas.size == preguntas.size,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = UtpRed)
                        ) {
                            if (enviando) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            else Text("ENVIAR RESPUESTAS", fontWeight = FontWeight.Bold)
                        }
                        if (preguntas.isNotEmpty() && elegidas.size < preguntas.size) {
                            Text(
                                "Responde todas las preguntas para enviar (${elegidas.size}/${preguntas.size}).",
                                fontSize = 11.sp,
                                color = UtpTextSecondary
                            )
                        }
                    } else {
                        Button(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = UtpPurplePrimary)
                        ) { Text("VOLVER AL CONTENIDO", fontWeight = FontWeight.Bold) }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
