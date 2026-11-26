package com.example.myapplication.data.local

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.myapplication.data.model.Material
import com.example.myapplication.data.remote.ApiClient
import org.json.JSONArray
import java.io.File

/**
 * Material descargado en el almacenamiento interno de la app.
 * Una vez descargado se puede abrir SIN conexión a internet.
 * Además de los archivos se guarda un índice (indice.json) con los datos de cada uno,
 * para poder listarlos aunque no haya servidor.
 */
object DescargasLocales {

    private fun carpeta(context: Context) = File(context.filesDir, "material").apply { mkdirs() }

    fun archivo(context: Context, material: Material): File {
        val seguro = material.nombreArchivo.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(carpeta(context), "${material.id}_$seguro")
    }

    fun estaDescargado(context: Context, material: Material) = archivo(context, material).exists()

    suspend fun descargar(context: Context, material: Material) {
        ApiClient.descargarMaterial(material.id, archivo(context, material))
        guardarEnIndice(context, material)
    }

    fun eliminar(context: Context, material: Material) {
        archivo(context, material).delete()
        val resto = leerIndice(context).filter { it.id != material.id }
        escribirIndice(context, resto)
    }

    /** Material descargado y disponible en este teléfono (funciona sin internet). */
    fun listar(context: Context): List<Material> =
        leerIndice(context).filter { archivo(context, it).exists() }

    /** Abre el archivo con la aplicación del teléfono que corresponda (visor de PDF, galería...). */
    fun abrir(context: Context, material: Material): Boolean {
        val file = archivo(context, material)
        if (!file.exists()) return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, material.contentType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false // no hay ninguna app instalada que abra este tipo de archivo
        }
    }

    // ---------- Índice ----------

    private fun indice(context: Context) = File(carpeta(context), "indice.json")

    @Synchronized
    private fun leerIndice(context: Context): List<Material> {
        val f = indice(context)
        if (!f.exists()) return emptyList()
        return try {
            val arr = JSONArray(f.readText())
            List(arr.length()) { Material.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    private fun escribirIndice(context: Context, lista: List<Material>) {
        val arr = JSONArray()
        lista.forEach { arr.put(it.toJson()) }
        indice(context).writeText(arr.toString())
    }

    private fun guardarEnIndice(context: Context, material: Material) {
        val lista = leerIndice(context).filter { it.id != material.id } + material
        escribirIndice(context, lista)
    }
}
