package com.example.kamara

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Firebase-tik irakurritako errezeten zerrenda; txartel trinkoak, klik egitean zabaltzen direnak. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ErrezetakPantaila(
    vm: ErrezetaViewModel,
    onAtzera: () -> Unit,
    onGehitu: () -> Unit,
    onEditatu: (Errezeta) -> Unit
) {
    val errezetak by vm.errezetak.collectAsStateWithLifecycle()
    val kargatzen by vm.kargatzen.collectAsStateWithLifecycle()
    val errorea by vm.errorea.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Errezetak") },
                navigationIcon = {
                    IconButton(onClick = onAtzera) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atzera")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onGehitu) {
                Icon(Icons.Default.Add, contentDescription = "Errezeta berria")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                kargatzen -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorea != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Errorea: ${errorea}")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { vm.erroreaGarbitu(); vm.kargatu() }) {
                            Text("Saiatu berriro")
                        }
                    }
                }
                errezetak.isEmpty() -> {
                    Text(
                        "Oraindik ez dago errezetarik\n+ botoiarekin gehitu edo Kamera erabili",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        items(errezetak, key = { it.id }) { e ->
                            ErrezetaTxartela(
                                errezeta = e,
                                onEditatu = onEditatu,
                                onEzabatu = { vm.ezabatu(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Errezeta txartel trinkoa. Itxita dagoenean izenburua eta laburpen laburra soilik erakusten
 * ditu; gainean klik egitean eduki osoa zabaltzen da. Bigarren klik batekin itxi egiten da.
 */
@Composable
private fun ErrezetaTxartela(
    errezeta: Errezeta,
    onEditatu: (Errezeta) -> Unit,
    onEzabatu: (Errezeta) -> Unit
) {
    var zabalik by remember { mutableStateOf(false) }

    val ocr = errezeta.testua.isNotBlank()
    val lerroak = if (ocr) {
        errezeta.testua.lines().map { it.trim() }.filter { it.isNotBlank() }
    } else {
        emptyList()
    }

    // Itxitako txartelaren izenburua: OCR testuaren lehen lerroa, edo formularioaren izena.
    val izenburua = if (ocr) {
        lerroak.firstOrNull() ?: "OCR testua"
    } else {
        errezeta.izena.ifBlank { "Errezeta" }
    }

    // Laburpena: OCR testuaren gainerako lerroak, edo formularioaren osagaiak.
    val laburpena = if (ocr) {
        lerroak.drop(1).joinToString(" · ")
    } else {
        errezeta.osagaiak.joinToString(", ")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { zabalik = !zabalik }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Izenburua + zabaltzeko gezia
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    izenburua,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (zabalik) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (zabalik) "Itxi" else "Ireki",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (zabalik) {
                // Zabalduta: eduki osoa
                Spacer(Modifier.height(8.dp))
                if (ocr) {
                    Text(errezeta.testua, style = MaterialTheme.typography.bodyMedium)
                } else {
                    if (errezeta.osagaiak.isNotEmpty()) {
                        Text("Osagaiak:", style = MaterialTheme.typography.labelLarge)
                        errezeta.osagaiak.forEach {
                            Text("• $it", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (errezeta.pausoak.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Sukaldatzeko pausoak:", style = MaterialTheme.typography.labelLarge)
                        errezeta.pausoak.forEachIndexed { i, p ->
                            Text("${i + 1}. $p", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else if (laburpena.isNotBlank()) {
                // Itxita: gehienez 2 lerro, "…" bukaeran
                Spacer(Modifier.height(4.dp))
                Text(
                    laburpena,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Ekintzak: arkatz-botoia eta ezabatzeko botoia
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onEditatu(errezeta) }) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editatu"
                    )
                }
                IconButton(onClick = { onEzabatu(errezeta) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Ezabatu")
                }
            }
        }
    }
}
