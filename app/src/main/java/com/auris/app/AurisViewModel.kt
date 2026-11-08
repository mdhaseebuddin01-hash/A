package com.auris.app

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

sealed interface ModelState {
    data object Loading : ModelState
    data class Ready(val classifier: Classifier) : ModelState
    data class Missing(val message: String) : ModelState
}

data class Analysis(
    val predictions: List<Prediction>,
    val quality: ImageQuality.Report,
    val confident: Boolean
) {
    val top: Prediction get() = predictions.first()
}

class AurisViewModel(app: Application) : AndroidViewModel(app) {

    private val log = CaseLog(app)
    private val saveLock = Mutex()

    var model by mutableStateOf<ModelState>(ModelState.Loading)
        private set
    var image by mutableStateOf<Bitmap?>(null)
        private set
    var analysis by mutableStateOf<Analysis?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** The diagnosis the doctor confirmed for the current case, once saved. */
    var verdictSaved by mutableStateOf<String?>(null)
        private set
    var cases by mutableStateOf<List<CaseRecord>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            cases = withContext(Dispatchers.IO) { log.load() }
            model = withContext(Dispatchers.IO) {
                try {
                    ModelState.Ready(Classifier.load(getApplication<Application>()))
                } catch (e: FileNotFoundException) {
                    ModelState.Missing(
                        "No model is installed yet. Train one with the Colab notebook, copy auris.onnx, " +
                            "labels.txt and model_info.json into app/src/main/assets, then rebuild."
                    )
                } catch (e: Exception) {
                    ModelState.Missing(
                        "The model couldn’t be loaded (${e.javaClass.simpleName}). Export it again with the notebook and rebuild."
                    )
                }
            }
            if (image != null && analysis == null) runAnalysis()
        }
    }

    fun onImage(uri: Uri) {
        viewModelScope.launch {
            busy = true
            error = null
            analysis = null
            verdictSaved = null
            val bitmap = withContext(Dispatchers.IO) {
                runCatching { ImageLoader.load(getApplication<Application>(), uri) }.getOrNull()
            }
            if (bitmap == null) {
                busy = false
                error = "This image couldn’t be opened. Try a JPG or PNG photo."
                return@launch
            }
            image = bitmap
            runAnalysis()
        }
    }

    private suspend fun runAnalysis() {
        val bitmap = image ?: return
        val ready = model as? ModelState.Ready
        if (ready == null) {
            busy = false
            return
        }
        busy = true
        val result = withContext(Dispatchers.Default) {
            runCatching {
                val predictions = ready.classifier.classify(bitmap)
                Analysis(
                    predictions = predictions,
                    quality = ImageQuality.check(bitmap),
                    confident = predictions.first().prob >= ready.classifier.info.threshold
                )
            }
        }
        result
            .onSuccess { analysis = it }
            .onFailure { error = "The analysis failed (${it.javaClass.simpleName}). Try another photo." }
        busy = false
    }

    fun saveVerdict(actual: String) {
        val a = analysis ?: return
        if (verdictSaved != null) return
        val now = System.currentTimeMillis()
        val record = CaseRecord(
            id = now.toString(36) + (1000..9999).random(),
            ts = now,
            ai = a.top.id,
            prob = a.top.prob,
            confident = a.confident,
            actual = actual,
            ok = actual == a.top.id,
            top3 = a.predictions.take(3).any { it.id == actual }
        )
        cases = listOf(record) + cases
        verdictSaved = actual
        persist()
    }

    fun clearLog() {
        cases = emptyList()
        persist()
    }

    private fun persist() {
        val snapshot = cases
        viewModelScope.launch(Dispatchers.IO) {
            saveLock.withLock { log.save(snapshot) }
        }
    }

    fun exportCsv(uri: Uri, done: (Boolean) -> Unit) {
        val text = Stats.csv(cases)
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val stream = getApplication<Application>().contentResolver.openOutputStream(uri)
                        ?: return@runCatching false
                    stream.use { it.write(text.toByteArray()) }
                    true
                }.getOrDefault(false)
            }
            done(ok)
        }
    }

    fun newCase() {
        image = null
        analysis = null
        error = null
        verdictSaved = null
        busy = false
    }
}
