package com.auris.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.auris.app.Analysis
import com.auris.app.AurisViewModel
import com.auris.app.Catalogue
import com.auris.app.ModelInfo
import com.auris.app.ModelState
import com.auris.app.Prediction
import com.auris.app.R
import java.io.File

@Composable
fun AnalyseScreen(vm: AurisViewModel) {
    val context = LocalContext.current
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.onImage(uri)
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = cameraUri
        if (saved && uri != null) vm.onImage(uri)
    }
    val chooseImage: () -> Unit = {
        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val capturePhoto: () -> Unit = {
        val uri = newCaptureUri(context)
        cameraUri = uri
        try {
            takePhoto.launch(uri)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No camera app was found on this phone.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Header()
        val model = vm.model
        if (model is ModelState.Missing) Banner(model.message, Tone.Warning)

        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Speculum(image = vm.image, busy = vm.busy, onClick = chooseImage)
        }
        Text(
            text = if (vm.image == null) {
                "Add a photo of the eardrum from your otoscope or endoscope."
            } else {
                "Tap the image to change it."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 26.dp)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = capturePhoto,
                enabled = !vm.busy,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Icon(painterResource(R.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Take photo")
            }
            FilledTonalButton(
                onClick = chooseImage,
                enabled = !vm.busy,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Icon(painterResource(R.drawable.ic_gallery), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Choose image")
            }
        }

        vm.error?.let { Banner(it, Tone.Error) }

        val analysis = vm.analysis
        when {
            vm.busy -> Text(
                "Reading the image…",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp)
            )
            analysis != null -> ResultSection(vm, analysis)
            vm.image != null && model is ModelState.Loading -> Text(
                "Loading the model…",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 20.dp)
            )
        }
    }
}

private fun newCaptureUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/** The image shown as if through an otoscope: a black circular field with a warm rim of light. */
@Composable
private fun Speculum(image: Bitmap?, busy: Boolean, onClick: () -> Unit) {
    val imageBitmap = remember(image) { image?.asImageBitmap() }
    Box(
        modifier = Modifier
            .size(272.dp)
            .shadow(elevation = 18.dp, shape = CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(8.dp)
            .clip(CircleShape)
            .background(Color.Black)
            .clickable(enabled = !busy, onClickLabel = "Choose an ear image", onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = "Selected ear image",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Canvas(Modifier.fillMaxSize()) { drawEardrum() }
        }
        Box(
            Modifier
                .fillMaxSize()
                .border(1.5.dp, Reflex.copy(alpha = 0.38f), CircleShape)
        )
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = Reflex,
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun ResultSection(vm: AurisViewModel, a: Analysis) {
    val info = (vm.model as? ModelState.Ready)?.classifier?.info
    val top = a.top
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 28.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(4.dp))
        a.quality.hints.forEach { Banner(it, Tone.Warning) }

        if (Catalogue.isQualityLabel(top.id)) {
            Text(
                "Can’t assess this image",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                "Retake it: clear wax or discharge first, fill the frame with the eardrum and hold steady.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Text(
                if (a.confident) "Most likely" else "Leading possibility",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                Catalogue.name(top.id),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            ConfidencePill(top.prob, a.confident)
            Text(
                confidenceNote(a.confident, info),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp)
            )
            Catalogue.signs(top.id)?.let { SignsCard(it) }
            TopThree(a.predictions.take(3))
            VerdictCard(vm, a)
        }

        Text(
            "Decision support only. Confirm clinically before treatment.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 20.dp)
        )
        OutlinedButton(
            onClick = { vm.newCase() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(50.dp)
        ) { Text("Start a new case") }
    }
}

private fun confidenceNote(confident: Boolean, info: ModelInfo?): String {
    val accuracy = info?.confidentAccuracy
    return when {
        confident && accuracy != null ->
            "On held-out test images, answers at this confidence were right ${pct(accuracy)}% of the time."
        confident -> "Above this model’s confidence threshold."
        else -> "Below the confidence threshold. Treat this as a prompt for specialist review, not a diagnosis."
    }
}

@Composable
private fun ConfidencePill(prob: Float, confident: Boolean) {
    val container = if (confident) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    val content = if (confident) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(50),
        modifier = Modifier.padding(top = 10.dp)
    ) {
        Text(
            (if (confident) "Confident: " else "Needs review: ") + "${pct(prob.toDouble())}%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SignsCard(signs: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "Typical signs to confirm",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                signs,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TopThree(predictions: List<Prediction>) {
    Column(Modifier.padding(top = 24.dp)) {
        Text(
            "Top 3 possibilities",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        predictions.forEach { p ->
            Column(Modifier.padding(top = 10.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        Catalogue.name(p.id),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${pct(p.prob.toDouble())}%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LinearProgressIndicator(
                    progress = { p.prob.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun VerdictCard(vm: AurisViewModel, a: Analysis) {
    var choosing by remember(a) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Was Auris right?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            val saved = vm.verdictSaved
            when {
                saved != null -> Text(
                    if (saved == a.top.id) "Verdict saved: Auris was right." else "Verdict saved: ${Catalogue.name(saved)}.",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                !choosing -> {
                    Text(
                        "Your answer adds this case to the accuracy score.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { vm.saveVerdict(a.top.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) { Text("Yes, correct") }
                        OutlinedButton(
                            onClick = { choosing = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) { Text("No, it’s different") }
                    }
                }
                else -> {
                    Text(
                        "Tap the confirmed diagnosis.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
                    )
                    (Catalogue.conditions + Catalogue.other)
                        .filter { it.id != a.top.id }
                        .forEach { c ->
                            Text(
                                c.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.saveVerdict(c.id) }
                                    .padding(vertical = 12.dp)
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    TextButton(onClick = { choosing = false }) { Text("Cancel") }
                }
            }
        }
    }
}
