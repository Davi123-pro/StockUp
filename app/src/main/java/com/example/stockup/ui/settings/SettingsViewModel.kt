package com.example.stockup.ui.settings

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockup.data.ProductRepository
import com.example.stockup.data.backup.BackupFiles
import com.example.stockup.data.backup.BackupManager
import com.example.stockup.data.backup.LocalBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Estado da cópia de segurança, para a tela mostrar o que aconteceu. */
data class BackupUiState(
    val isWorking: Boolean = false,
    val message: String? = null,
    /** Cópias guardadas dentro do aplicativo — restauração em dois toques. */
    val localFiles: List<LocalBackup> = emptyList(),
    val isScanning: Boolean = false
)

class SettingsViewModel(
    private val repository: ProductRepository,
    private val appContext: Context
) : ViewModel() {

    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    init {
        refreshLocalFiles()
    }

    fun clearBackupMessage() {
        _backupState.value = _backupState.value.copy(message = null)
    }

    // ------------------------------------------------------------ salvar cópia

    /**
     * Grava uma cópia com os dados atuais DENTRO do aplicativo.
     *
     * Por que não grava mais fora: o dono do produto pediu para tirar a gravação
     * em pastas do celular (seletor de pasta e compartilhamento) e decidir depois
     * outra forma de exportar. Então este é o único caminho que sobrou — e ele
     * não pede permissão nenhuma, nem abre tela alguma.
     */
    fun salvarCopiaNoApp() {
        if (!iniciarTrabalho()) return

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val destino = BackupFiles.newFileFor(
                        appContext,
                        "${BackupManager.SUGGESTED_FILE_NAME}-${dataDeHoje()}"
                    )
                    escreverCopia(destino)
                    BackupFiles.prune(appContext)
                }
                _backupState.value = _backupState.value.copy(
                    isWorking = false,
                    message = "Cópia salva dentro do app."
                )
                refreshLocalFiles()
            } catch (e: Exception) {
                _backupState.value = _backupState.value.copy(
                    isWorking = false,
                    message = "Não consegui salvar a cópia. ${e.message.orEmpty()}".trim()
                )
            }
        }
    }

    /** Relê as cópias guardadas dentro do aplicativo. */
    fun refreshLocalFiles() {
        _backupState.value = _backupState.value.copy(isScanning = true)
        viewModelScope.launch {
            val locais = withContext(Dispatchers.IO) { BackupFiles.list(appContext) }
            _backupState.value = _backupState.value.copy(localFiles = locais, isScanning = false)
        }
    }

    // ------------------------------------------------------------ restaurar

    /** Restaura uma cópia guardada no aplicativo (o caminho de dois toques). */
    fun restoreFromLocal(copia: LocalBackup) {
        restaurar { copia.file.inputStream() }
    }

    /** Restaura um arquivo escolhido pelo usuário no seletor do Android. */
    fun restoreFromUri(resolver: ContentResolver, origem: Uri) {
        restaurar {
            resolver.openInputStream(origem)
                ?: throw IllegalStateException("Não consegui abrir o arquivo escolhido.")
        }
    }

    private fun restaurar(abrir: suspend () -> InputStream) {
        if (!iniciarTrabalho()) return

        viewModelScope.launch {
            try {
                // Ler a cópia ANTES de gravar a rede de segurança.
                //
                // Por que a ordem importa: a rede de segurança usa o MESMO nome
                // de arquivo da cópia "Antes de restaurar" mostrada na lista. Se
                // ela fosse gravada primeiro, sobrescreveria justamente o
                // arquivo escolhido — e o app restauraria o estado atual sobre
                // ele mesmo, apagando para sempre a cópia antiga. Foi assim que
                // uma cópia de 25/10 se perdeu durante os testes de 25/10.
                val bytes = withContext(Dispatchers.IO) {
                    abrir().use { it.readBytes() }
                }

                // Rede de segurança: guarda o estado ATUAL antes de substituir. É o que
                // permite restaurar em dois toques sem virar ação irreversível.
                val guardouAnterior = withContext(Dispatchers.IO) { salvarCopiaDeSeguranca() }

                val conteudo = withContext(Dispatchers.IO) {
                    java.io.ByteArrayInputStream(bytes).use { entrada ->
                        BackupManager.read(entrada, pastaDeFotos())
                    }
                }

                withContext(Dispatchers.IO) {
                    repository.deleteAllProducts()
                    repository.deleteAllShoppingItems()
                    repository.clearHistory()
                    repository.deleteAllLocations()

                    // Insere preservando os ids originais (OnConflictStrategy.REPLACE).
                    conteudo.products.forEach { repository.insertProduct(it) }
                    conteudo.shopping.forEach { repository.insertShoppingItem(it) }
                    conteudo.history.forEach { repository.insertConsumptionRecord(it) }
                    conteudo.locations.forEach { repository.insertLocation(it) }
                }

                val aviso = if (guardouAnterior) {
                    " A sua despensa de antes ficou guardada como \"Antes de restaurar\"."
                } else {
                    ""
                }
                _backupState.value = _backupState.value.copy(
                    isWorking = false,
                    message = "Pronto! ${conteudo.products.size} produtos e " +
                        "${conteudo.photos} fotos voltaram.$aviso"
                )
                refreshLocalFiles()
            } catch (e: IllegalArgumentException) {
                _backupState.value = _backupState.value.copy(
                    isWorking = false,
                    message = e.message ?: "Este arquivo não é uma cópia do StockUp."
                )
            } catch (e: Exception) {
                _backupState.value = _backupState.value.copy(
                    isWorking = false,
                    message = "Não consegui restaurar. ${e.message.orEmpty()}".trim()
                )
            }
        }
    }

    // ------------------------------------------------------------ limpeza

    fun clearAllProducts() {
        viewModelScope.launch {
            val products = repository.allProducts.first()
            products.forEach {
                repository.deleteProduct(it)
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // ------------------------------------------------------------ apoio

    private suspend fun escreverCopia(destino: File) {
        destino.outputStream().use { saida -> escreverCopia(saida) }
    }

    private suspend fun escreverCopia(saida: OutputStream) {
        BackupManager.write(
            out = saida,
            products = repository.allProducts.first(),
            shopping = repository.allShoppingItems.first(),
            history = repository.allHistory.first(),
            locations = repository.allLocations.first()
        )
    }

    private fun pastaDeFotos(): File =
        File(appContext.filesDir, BackupManager.PHOTOS_DIR_NAME)

    /** Devolve false se já existe uma operação em andamento (evita toque duplo). */
    private fun iniciarTrabalho(): Boolean {
        if (_backupState.value.isWorking) return false
        _backupState.value = _backupState.value.copy(isWorking = true, message = null)
        return true
    }

    /**
     * Grava uma cópia do estado atual com o sufixo "antes-de-restaurar", dentro do
     * próprio aplicativo — assim ela aparece na lista de restauração.
     */
    private suspend fun salvarCopiaDeSeguranca(): Boolean = runCatching {
        val nome = "${BackupManager.SUGGESTED_FILE_NAME}-${BackupFiles.ANTES_DE_RESTAURAR}"
        escreverCopia(BackupFiles.newFileFor(appContext, nome))
        BackupFiles.prune(appContext)
        true
    }.getOrDefault(false)

    private fun dataDeHoje(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}
