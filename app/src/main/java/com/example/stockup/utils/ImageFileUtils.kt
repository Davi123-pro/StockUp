package com.example.stockup.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageFileUtils {

    /**
     * Cria um arquivo temporário seguro no diretório de cache interno.
     * Não requer permissões de armazenamento externo e funciona em qualquer versão do Android.
     */
    fun createSafeCameraUri(context: Context): Uri {
        val cacheFolder = File(context.cacheDir, "camera_photos").apply {
            if (!exists()) mkdirs()
        }
        val tempFile = File(cacheFolder, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    /**
     * Otimiza a foto (câmera ou galeria):
     * 1. Redimensiona para no máximo 1080x1080 (evita OutOfMemory em aparelhos com 4GB RAM)
     * 2. Corrige a rotação EXIF da câmera da Samsung
     * 3. Salva com compressão de alta qualidade no armazenamento interno definitivo do app
     * 4. Remove o arquivo temporário original da câmera
     */
    fun compressAndSaveOptimizedImage(context: Context, sourceUri: Uri): Uri? {
        return try {
            val contentResolver = context.contentResolver

            // 1. Lê as dimensões da imagem sem carregá-la toda na memória
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            val outWidth = options.outWidth
            val outHeight = options.outHeight
            if (outWidth <= 0 || outHeight <= 0) return sourceUri

            // 2. Calcula fator de escala para manter o maior lado em no máximo 1080px
            val maxDimension = 1080
            var inSampleSize = 1
            val maxOriginal = max(outWidth, outHeight)
            while (maxOriginal / (inSampleSize * 2) >= maxDimension) {
                inSampleSize *= 2
            }

            // 3. Decodifica o Bitmap na resolução ideal
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val loadedBitmap: Bitmap = contentResolver.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return sourceUri

            // 4. Corrige a orientação EXIF (muito importante para câmeras Samsung)
            val bitmap = correctExifRotation(context, sourceUri, loadedBitmap)

            // 5. Salva na pasta permanente do app
            val destDir = File(context.filesDir, "product_images").apply {
                if (!exists()) mkdirs()
            }
            val destFile = File(destDir, "prod_${System.currentTimeMillis()}.jpg")

            FileOutputStream(destFile).use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 82, outStream)
            }

            // Recicla o bitmap da memória imediatamente
            bitmap.recycle()

            // 6. Limpa arquivo temporário do cache se aplicável
            if (sourceUri.scheme == "content") {
                try {
                    val path = sourceUri.path
                    if (path != null && path.contains("camera_photos")) {
                        val file = File(context.cacheDir, "camera_photos/${destFile.name}")
                        if (file.exists()) file.delete()
                    }
                } catch (ignored: Exception) { }
            }

            Uri.fromFile(destFile)
        } catch (e: Exception) {
            e.printStackTrace()
            // Se falhar a compressão, retorna a URI original como fallback seguro
            sourceUri
        }
    }

    private fun correctExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        var inputStream: InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(inputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
                )
                if (rotatedBitmap != bitmap) {
                    bitmap.recycle()
                }
                rotatedBitmap
            } else {
                bitmap
            }
        } catch (e: Exception) {
            bitmap
        } finally {
            try { inputStream?.close() } catch (ignored: Exception) { }
        }
    }
}
