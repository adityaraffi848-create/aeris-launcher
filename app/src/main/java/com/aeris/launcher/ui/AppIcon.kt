package com.aeris.launcher.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeris.launcher.LauncherViewModel
import com.aeris.launcher.data.AppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Aksi yang dibutuhkan menu tahan-lama pada ikon aplikasi. */
class AppActions(
    val vm: LauncherViewModel,
    val pickIcon: (AppItem) -> Unit,
    val openAerisSettings: () -> Unit,
)

/** Gambar ikon aplikasi (ikon custom jika ada), dengan bentuk sesuai pengaturan. */
@Composable
fun AppIconImage(app: AppItem, vm: LauncherViewModel, size: Dp) {
    val s = LocalSettings.current
    val bmp by produceState<ImageBitmap?>(null, app.key, vm.iconVersion) {
        value = withContext(Dispatchers.Default) { vm.icons.load(app).asImageBitmap() }
    }
    val shape = RoundedCornerShape(percent = s.iconShape.cornerPercent)
    Box(Modifier.size(size).clip(shape)) {
        bmp?.let { Image(it, contentDescription = app.label, modifier = Modifier.size(size), contentScale = ContentScale.Crop) }
    }
}

@Composable
fun AppIconCell(
    app: AppItem,
    actions: AppActions,
    modifier: Modifier = Modifier,
    iconSize: Dp = 58.dp,
    showLabel: Boolean = true,
    applyScale: Boolean = true,
    labelColor: Color = Color.White,
    onLaunched: (() -> Unit)? = null,
) {
    val vm = actions.vm
    val s = LocalSettings.current
    val size = if (applyScale) iconSize * s.iconScale else iconSize
    var menu by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, label = "press")

    Column(
        modifier
            .pointerInput(app.key) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = {
                        vm.launch(app)
                        onLaunched?.invoke()
                    },
                    onLongPress = { menu = true },
                )
            }
            .graphicsLayer { scaleX = scale; scaleY = scale },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            AppIconImage(app, vm, size)
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                val inDock = app.key in vm.dockKeys
                val inHome = app.key in vm.homeKeys
                DropdownMenuItem(text = { Text("Ganti ikon…") }, onClick = { menu = false; actions.pickIcon(app) })
                if (vm.icons.hasCustom(app)) {
                    DropdownMenuItem(text = { Text("Reset ikon") }, onClick = { menu = false; vm.resetIcon(app) })
                }
                DropdownMenuItem(
                    text = { Text(if (inDock) "Lepas dari Dock" else "Taruh di Dock") },
                    onClick = { menu = false; vm.toggleDock(app) },
                )
                DropdownMenuItem(
                    text = { Text(if (inHome) "Hapus dari Layar Utama" else "Tambah ke Layar Utama") },
                    onClick = { menu = false; vm.toggleHome(app) },
                )
                DropdownMenuItem(text = { Text("Info aplikasi") }, onClick = { menu = false; vm.openAppInfo(app) })
                DropdownMenuItem(text = { Text("Copot pemasangan") }, onClick = { menu = false; vm.uninstall(app) })
                DropdownMenuItem(text = { Text("Pengaturan Aeris") }, onClick = { menu = false; actions.openAerisSettings() })
            }
        }
        if (showLabel) {
            Text(
                text = app.label,
                color = labelColor,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = if (labelColor == Color.White) LabelShadow else null),
                modifier = Modifier.width(size + 14.dp),
            )
        }
    }
}
