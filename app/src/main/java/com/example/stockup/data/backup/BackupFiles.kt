package com.example.stockup.data.backup

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Uma cópia guardada dentro do próprio aplicativo. */
data class LocalBackup(
    val file: File,
    val label: String,
    val modifiedAt: Long
)

/**
 * Guarda cópias dentro do armazenamento interno do aplicativo.
 *
 * Serve a dois propósitos:
 * 1. É o arquivo que o app manda pelo "Compartilhar" (WhatsApp, Drive, e-mail...).
 * 2. Fica uma cópia local para o usuário restaurar sem abrir seletor de arquivos
 *    nenhum — dois toques, sem navegar por pastas.
 *
 * Mantém apenas as [KEEP] mais recentes: cópia de segurança velha demais não ajuda
 * e ocupa espaço do celular.
 */
object BackupFiles {

    /**
     * Sufixo da cópia automática gravada antes de uma restauração.
     *
     * Mora aqui desde que o app deixou de gravar em pastas do celular: este é o
     * único lugar que ainda cria cópias.
     */
    const val ANTES_DE_RESTAURAR = "antes-de-restaurar"

    private const val DIR = "backups"
    private const val KEEP = 5

    fun dir(context: Context): File =
        File(context.filesDir, DIR).apply { if (!exists()) mkdirs() }

    /** Cópias locais, da mais nova para a mais antiga. */
    fun list(context: Context): List<LocalBackup> =
        dir(context).listFiles()
            ?.filter { it.isFile && it.name.startsWith(BackupManager.SUGGESTED_FILE_NAME) }
            ?.map { arquivo ->
                LocalBackup(
                    file = arquivo,
                    label = rotuloDaCopia(arquivo.name, arquivo.lastModified()),
                    modifiedAt = arquivo.lastModified()
                )
            }
            ?.sortedByDescending { it.modifiedAt }
            ?: emptyList()

    /**
     * Cria (ou reaproveita) o arquivo de uma cópia. Apaga antes uma cópia do mesmo
     * dia, para não acumular "stockup-copia-2026-10-13 (1).zip".
     */
    fun newFileFor(context: Context, baseName: String): File {
        val pasta = dir(context)
        pasta.listFiles()
            ?.filter { it.name.startsWith(baseName) }
            ?.forEach { runCatching { it.delete() } }
        return File(pasta, "$baseName.zip")
    }

    /** Mantém só as [KEEP] cópias mais recentes. */
    fun prune(context: Context, keep: Int = KEEP) {
        list(context).drop(keep).forEach { runCatching { it.file.delete() } }
    }
}

/**
 * "stockup-copia-2026-10-13.zip" → "Cópia de 13/10/2026".
 * A cópia automática (gravada antes de uma restauração) recebe um rótulo próprio.
 */
fun rotuloDaCopia(fileName: String, modifiedAt: Long): String {
    val data = dataDoNome(fileName) ?: Date(modifiedAt)
    val texto = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(data)
    return if (fileName.contains(BackupFiles.ANTES_DE_RESTAURAR)) {
        "Antes de restaurar — $texto"
    } else {
        "Cópia de $texto"
    }
}

private fun dataDoNome(fileName: String): Date? {
    val encontrado = Regex("""(\d{4})-(\d{2})-(\d{2})""").find(fileName) ?: return null
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(encontrado.value)
    }.getOrNull()
}
