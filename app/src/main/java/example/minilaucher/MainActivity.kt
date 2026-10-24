package com.example.minilauncher

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppItem(val label: String, val pkg: String, val icon: ImageBitmap)

class MainActivity : ComponentActivity() {

    private var refreshTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LauncherScreen(refreshTick) }
    }

    // Se recarga al volver (por si se instalaron/desinstalaron apps)
    override fun onResume() {
        super.onResume()
        refreshTick++
    }
}

private fun loadApps(context: Context): List<AppItem> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .map {
            AppItem(
                label = it.loadLabel(pm).toString(),
                pkg = it.activityInfo.packageName,
                icon = it.loadIcon(pm).toBitmap(128, 128).asImageBitmap()
            )
        }
        .sortedBy { it.label.lowercase() }
}

@Composable
fun LauncherScreen(refreshTick: Int) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(refreshTick) {
        apps = withContext(Dispatchers.Default) { loadApps(context) }
    }

    // En un launcher, "atrás" no debe cerrar la pantalla de inicio
    BackHandler { query = "" }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar app…", color = Color.White.copy(alpha = 0.6f)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                cursorColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(76.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filtered, key = { it.pkg }) { app ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable {
                            context.packageManager
                                .getLaunchIntentForPackage(app.pkg)
                                ?.let { context.startActivity(it) }
                        }
                        .padding(vertical = 10.dp, horizontal = 4.dp)
                ) {
                    Image(
                        bitmap = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        app.label,
                        color = Color.White,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
