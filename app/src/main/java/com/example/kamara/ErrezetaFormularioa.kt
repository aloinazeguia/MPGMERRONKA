package com.example.kamara

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** Errezeta berria sortzeko formularioa: argazkia (aukerakoa), osagaiak eta pausoak. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrezetaFormularioa(
    vm: ErrezetaViewModel,
    hasierakoArgazkia: Uri?,
    onAtzera: () -> Unit,
    onAmaituta: () -> Unit
) {
    var izena by remember { mutableStateOf("") }
    var argazkia by remember { mutableStateOf(hasierakoArgazkia) }
    var osagaiakTestua by remember { mutableStateOf("") } // lerro bakoitzeko osagai bat
    var pausoakTestua by remember { mutableStateOf("") }  // lerro bakoitzeko pauso bat
    var gordetzen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Errezeta berria") },
                navigationIcon = {
                    IconButton(onClick = onAtzera) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atzera")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp)
        ) {

        if (argazkia != null) {
            AsyncImage(
                model = argazkia,
                contentDescription = "Errezetaren argazkia",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Argazkia gordeko da Firebasen",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                "Argazkirik gabe gordeko da (aukerakoa)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = izena,
            onValueChange = { izena = it },
            label = { Text("Errezetaren izena") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = osagaiakTestua,
            onValueChange = { osagaiakTestua = it },
            label = { Text("Osagaiak (lerro bakoitzeko bat)") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = pausoakTestua,
            onValueChange = { pausoakTestua = it },
            label = { Text("Sukaldatzeko pausoak (lerro bakoitzeko bat)") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                if (izena.isNotBlank() && !gordetzen) {
                    gordetzen = true
                    vm.gorde(
                        Errezeta(
                            izena = izena.trim(),
                            osagaiak = osagaiakTestua.lines()
                                .map { it.trim() }.filter { it.isNotEmpty() },
                            pausoak = pausoakTestua.lines()
                                .map { it.trim() }.filter { it.isNotEmpty() }
                        ),
                        argazkia
                    ) { onAmaituta() }
                }
            },
            enabled = izena.isNotBlank() && !gordetzen,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(if (gordetzen) "Gordetzen..." else "Gorde Firebasen")
        }
        }
    }
}
