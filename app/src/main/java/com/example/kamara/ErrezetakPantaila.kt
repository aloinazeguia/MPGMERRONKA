package com.example.kamara

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

/** Firebase-tik irakurritako errezeten zerrenda. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ErrezetakPantaila(
    vm: ErrezetaViewModel,
    onAtzera: () -> Unit,
    onGehitu: () -> Unit
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
                        androidx.compose.material3.Button(onClick = { vm.erroreaGarbitu(); vm.kargatu() }) {
                            Text("Saiatu berriro")
                        }
                    }
                }
                errezetak.isEmpty() -> {
                    Text(
                        "Oraindik ez dago errezetarik\n+ botoiarekin gehitu edo Kamera erabili",
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        items(errezetak, key = { it.id }) { e ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    e.argazkiaUrl?.let { url ->
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "Argazkia: ${e.izena}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    Text(e.izena, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Osagaiak: ${e.osagaiak.joinToString(", ").ifEmpty { "—" }}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Pausoak (${e.pausoak.size})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    e.pausoak.forEachIndexed { i, p ->
                                        Text("${i + 1}. $p", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = { vm.ezabatu(e) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Ezabatu")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
