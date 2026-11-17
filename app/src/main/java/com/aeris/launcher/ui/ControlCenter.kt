package com.aeris.launcher.ui

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeris.launcher.LauncherViewModel
import com.aeris.launcher.data.ThemeMode
import com.aeris.launcher.system.SystemStatus

/** Slider vertikal: dipakai di Pusat Kontrol dan pop up volume. */
@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: String,
    modifier: Modifier = Modifier,
    trackColor: Color = Color(0x40FFFFFF),
) {
    val v = value.coerceIn(0f, 1f)
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(trackColor)
            .pointerInput(Unit) {
                detectTapGestures { off -> onValueChange((1f - off.y / size.height).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    onValueChange((1f - change.position.y / size.height).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(Modifier.fillMaxWidth().fillMaxHeight(v).background(Color.White))
        Text(
            icon,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}

@Composable
private fun Tile(icon: String, label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) p.accent else p.sheetItem)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 22.sp)
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (active) Color.White else p.sheetText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WideButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(p.sheetItem)
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = p.sheetText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** Pusat Kontrol ala iOS: tile cepat + slider volume vertikal. */
@Composable
fun ControlCenter(
    vm: LauncherViewModel,
    status: SystemStatus,
    topPad: Dp,
    onClose: () -> Unit,
    onOpenAeris: () -> Unit,
) {
    val p = LocalPalette.current
    LaunchedEffect(Unit) { vm.refreshVolumes() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .pointerInput(Unit) { detectTapGestures { onClose() } }
            .pointerInput(Unit) {
                var dy = 0f
                detectVerticalDragGestures(
                    onDragStart = { dy = 0f },
                    onDragEnd = { if (dy < -60f) onClose() },
                    onVerticalDrag = { _, d -> dy += d },
                )
            },
    ) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = topPad + 8.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(p.sheetBg)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Tile("📶", "Wi-Fi", status.wifi, Modifier.weight(1f)) { vm.openWifiPanel() }
                        Tile("📡", "Data", status.cellular, Modifier.weight(1f)) { vm.openSettings(Settings.ACTION_WIRELESS_SETTINGS) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Tile("🔵", "Bluetooth", false, Modifier.weight(1f)) { vm.openSettings(Settings.ACTION_BLUETOOTH_SETTINGS) }
                        Tile("🔦", "Senter", vm.torch.on, Modifier.weight(1f)) {
                            if (vm.torch.available) vm.torch.toggle() else vm.toast("HP ini tidak punya senter")
                        }
                    }
                }
                val track = if (p.mode == ThemeMode.NORMAL) Color(0x33000000) else Color(0x40FFFFFF)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        VerticalSlider(vm.mediaVolume, { vm.changeMediaVolume(it) }, "🔊", Modifier.width(56.dp).height(158.dp), track)
                    VerticalSlider(vm.ringVolume, { vm.changeRingVolume(it) }, "🔔", Modifier.width(56.dp).height(158.dp), track)
                }
            }
            Spacer(Modifier.size(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WideButton("⚙️  Pengaturan Aeris", Modifier.weight(1f)) { onClose(); onOpenAeris() }
                WideButton("Pengaturan Android", Modifier.weight(1f)) { vm.openSettings(Settings.ACTION_SETTINGS) }
            }
        }
    }
}
