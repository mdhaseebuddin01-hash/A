package com.auris.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.auris.app.AurisViewModel
import com.auris.app.Catalogue
import com.auris.app.Condition
import com.auris.app.ModelState

@Composable
fun ConditionsScreen(vm: AurisViewModel) {
    val labels = (vm.model as? ModelState.Ready)?.classifier?.labels?.toSet()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Header() }
        item {
            Column(Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                Text(
                    "Conditions covered",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (labels == null) {
                        "The conditions Auris is designed to recognise. Tap one to see the signs it looks for."
                    } else {
                        "Tap a condition to see its signs. Conditions marked “Not in this model” need more training images."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        items(Catalogue.conditions, key = { it.id }) { condition ->
            ConditionCard(condition, inModel = labels?.contains(condition.id))
        }
        item { TipsCard() }
    }
}

@Composable
private fun ConditionCard(c: Condition, inModel: Boolean?) {
    var open by rememberSaveable(c.id) { mutableStateOf(false) }
    Card(
        onClick = { open = !open },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        c.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (inModel == false) {
                        Text(
                            "Not in this model",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                Text(
                    if (open) "−" else "+",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (open) {
                Text(
                    c.signs,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TipsCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Getting a clear image",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            listOf(
                "Clear wax or discharge first if it hides the eardrum.",
                "Fill the frame with the eardrum and hold steady before capturing.",
                "Angle the speculum slightly to cut glare from the light.",
                "Use one ear per photo."
            ).forEach { tip ->
                Text(
                    "•  $tip",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
