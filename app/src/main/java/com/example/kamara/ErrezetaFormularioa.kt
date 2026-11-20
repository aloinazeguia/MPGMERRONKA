package com.example.kamara

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Errezeta berria sortzeko formularioa.
 * QR-tik eskaneatutako datuekin dator [hasierakoErrezeta] (null bada, hutsik hasi).
 * Ez da argazkirik gordetzen (Storage ez da dohainik).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrezetaFormularioa(
    vm: ErrezetaViewModel,
    hasierakoErrezeta: Errezeta?,
    onAtzera: () -> Unit,
    onAmaituta: () -> Unit
) {
    val context = LocalContext.current

    // QR-tik etorritako datuekin hasi (edo hutsik, eskuz gehitzen bada)
    var izena by remember(hasierakoErrezeta) {
        mutableStateOf(hasierakoErrezeta?.izena ?: "")
    }
    var osagaiakTestua by remember(hasierakoErrezeta) {
        mutableStateOf(hasierakoErrezeta?.osagaiak?.joinToString("\n") ?: "")
    }
    var pausoakTestua by remember(hasierakoErrezeta) {
        mutableStateOf(hasierakoErrezeta?.pausoak?.joinToString("\n") ?: "")
    }
    var gordetzen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (hasierakoErrezeta != null) "QR-tik irakurritako errezeta" else "Errezeta berria")
                },
                navigationIcon = {
                    IconButton(onClick = onAtzera) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atzera")
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
            if (hasierakoErrezeta != null) {
                Text(
                    "Eskaneatutako datuak berrikusi eta gorde ditzakezu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
            }

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
                            errezeta = Errezeta(
                                izena = izena.trim(),
                                osagaiak = osagaiakTestua.lines()
                                    .map { it.trim() }.filter { it.isNotEmpty() },
                                pausoak = pausoakTestua.lines()
                                    .map { it.trim() }.filter { it.isNotEmpty() }
                            ),
                            onSuccess = {
                                gordetzen = false
                                onAmaituta()
                            },
                            onError = { errorMsg ->
                                gordetzen = false
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        )
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
