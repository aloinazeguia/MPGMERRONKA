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
 * Formularioaren atea. Hiru egoera daude:
 *  - `editatzenAriDenErrezeta != null` -> **edizioa**: errezeta horren eremuak beteta,
 *    eta gordetzean Firestore-eko dokumentu bera eguneratu.
 *  - `hasierakoTestua != null` -> **Kamera (OCR)**: testu osoa eremu bakarrean.
 *  - bestela -> **Errezetak → +**: formulario klasikoa (lehengo metodoa).
 */
@Composable
fun ErrezetaFormularioa(
    vm: ErrezetaViewModel,
    hasierakoTestua: String?,
    editatzenAriDenErrezeta: Errezeta?,
    onAtzera: () -> Unit,
    onAmaituta: () -> Unit
) {
    when {
        editatzenAriDenErrezeta != null -> {
            val e = editatzenAriDenErrezeta
            if (e.testua.isNotBlank()) {
                TestuFormularioa(vm, e.testua, e, onAtzera, onAmaituta)
            } else {
                ErrezetaFormularioKlasikoa(vm, e, onAtzera, onAmaituta)
            }
        }

        hasierakoTestua != null -> {
            TestuFormularioa(vm, hasierakoTestua, null, onAtzera, onAmaituta)
        }

        else -> {
            ErrezetaFormularioKlasikoa(vm, null, onAtzera, onAmaituta)
        }
    }
}

/** Errezetak pantailatik gehitzean/editatzean erabiltzen den formulario klasikoa. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrezetaFormularioKlasikoa(
    vm: ErrezetaViewModel,
    editatzen: Errezeta?,
    onAtzera: () -> Unit,
    onAmaituta: () -> Unit
) {
    val context = LocalContext.current
    var izena by remember(editatzen) { mutableStateOf(editatzen?.izena ?: "") }
    var osagaiakTestua by remember(editatzen) {
        mutableStateOf(editatzen?.osagaiak?.joinToString("\n") ?: "")
    }
    var pausoakTestua by remember(editatzen) {
        mutableStateOf(editatzen?.pausoak?.joinToString("\n") ?: "")
    }
    var gordetzen by remember { mutableStateOf(false) }

    val gorde: (Errezeta) -> Unit = { e ->
        gordetzen = true
        val onSuccess = {
            gordetzen = false
            onAmaituta()
        }
        val onError: (String) -> Unit = { msg ->
            gordetzen = false
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
        if (editatzen != null) vm.eguneratu(e, onSuccess, onError)
        else vm.gorde(e, onSuccess, onError)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editatzen != null) "Editatu errezeta" else "Errezeta berria") },
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
            OutlinedTextField(
                value = izena,
                onValueChange = { izena = it },
                label = { Text("Izena") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = osagaiakTestua,
                onValueChange = { osagaiakTestua = it },
                label = { Text("Osagaiak (lerro bakoitzeko bat)") },
                minLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = pausoakTestua,
                onValueChange = { pausoakTestua = it },
                label = { Text("Sukaldatzeko pausoak (lerro bakoitzeko bat)") },
                minLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (izena.isNotBlank() && !gordetzen) {
                        gorde(
                            Errezeta(
                                id = editatzen?.id ?: "",
                                izena = izena.trim(),
                                osagaiak = osagaiakTestua.lines()
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() },
                                pausoak = pausoakTestua.lines()
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() },
                                sortuta = editatzen?.sortuta ?: System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled = izena.isNotBlank() && !gordetzen,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    if (gordetzen) "Gordetzen..."
                    else if (editatzen != null) "Gorde aldaketak"
                    else "Gorde Firebasen"
                )
            }
        }
    }
}

/** Kameraren OCR-ek irakurritako testua erakusten duen formularioa (eta haren edizioa). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestuFormularioa(
    vm: ErrezetaViewModel,
    hasierakoTestua: String,
    editatzen: Errezeta?,
    onAtzera: () -> Unit,
    onAmaituta: () -> Unit
) {
    val context = LocalContext.current
    var testua by remember(hasierakoTestua) { mutableStateOf(hasierakoTestua) }
    var gordetzen by remember { mutableStateOf(false) }

    val gorde: (Errezeta) -> Unit = { e ->
        gordetzen = true
        val onSuccess = {
            gordetzen = false
            onAmaituta()
        }
        val onError: (String) -> Unit = { msg ->
            gordetzen = false
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
        if (editatzen != null) vm.eguneratu(e, onSuccess, onError)
        else vm.gorde(e, onSuccess, onError)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            editatzen != null -> "Editatu testua"
                            else -> "Argazkitik irakurritako testua"
                        }
                    )
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
            Text(
                "Testu osoa gordeko da. Zuzendu nahi baduzu, editatu hemen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = testua,
                onValueChange = { testua = it },
                label = { Text("Errezetaren testua") },
                minLines = 8,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (testua.isNotBlank() && !gordetzen) {
                        gorde(
                            Errezeta(
                                id = editatzen?.id ?: "",
                                testua = testua.trim(),
                                sortuta = editatzen?.sortuta ?: System.currentTimeMillis()
                            )
                        )
                    }
                },
                enabled = testua.isNotBlank() && !gordetzen,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    if (gordetzen) "Gordetzen..."
                    else if (editatzen != null) "Gorde aldaketak"
                    else "Gorde Firebasen"
                )
            }
        }
    }
}
