package com.aeris.launcher.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeris.launcher.LauncherViewModel
import com.aeris.launcher.data.IconShape
import com.aeris.launcher.data.ThemeMode

@Composable
private fun SectionTitle(text: String) {
    val p = LocalPalette.current
    Text(
        text.uppercase(),
        color = p.sheetSub,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun Chip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier
            .clip(shape)
            .background(p.sheetItem)
            .border(2.dp, if (selected) p.accent else Color.Transparent, shape)
            .pointerInput(text) { detectTapGestures { onClick() } }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = p.sheetText, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = p.sheetText, fontSize = 15.sp)
            Text(subtitle, color = p.sheetSub, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun SettingsSheet(vm: LauncherViewModel, onPickPhoto: () -> Unit, onClose: () -> Unit) {
    val p = LocalPalette.current
    val s = vm.settings
    val ctx = LocalContext.current
    var cityText by remember { mutableStateOf(s.city) }
    var cityMsg by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(p.sheetBg)
                .pointerInput(Unit) { detectTapGestures { } }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp)
                .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pengaturan Aeris", color = p.sheetText, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(onClick = onClose) { Text("Selesai") }
            }

            SectionTitle("Mode tampilan")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemeMode.values().forEach { m ->
                    val icon = when (m) { ThemeMode.NORMAL -> "☀️"; ThemeMode.GLASS -> "✨"; ThemeMode.DARK -> "🌙" }
                    Chip("$icon\n${m.title}", s.theme == m, Modifier.weight(1f)) {
                        vm.updateSettings { it.copy(theme = m) }
                    }
                }
            }

            SectionTitle("Ikon")
            Text("Ukuran ikon", color = p.sheetText, fontSize = 15.sp)
            Slider(
                value = s.iconScale,
                onValueChange = { v -> vm.updateSettings { it.copy(iconScale = v) } },
                valueRange = 0.8f..1.25f,
            )
            Text("Bentuk ikon", color = p.sheetText, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconShape.values().forEach { sh ->
                    Chip(sh.title, s.iconShape == sh, Modifier.weight(1f)) {
                        vm.updateSettings { it.copy(iconShape = sh) }
                    }
                }
            }
            SwitchRow("Nama aplikasi", "Tampilkan label di bawah ikon", s.showLabels) { c ->
                vm.updateSettings { it.copy(showLabels = c) }
            }
            Text(
                "Tips: tahan ikon aplikasi lalu pilih “Ganti ikon…” untuk memakai foto/gambar sendiri.",
                color = p.sheetSub, fontSize = 12.sp,
            )

            SectionTitle("Cuaca")
            OutlinedTextField(
                value = cityText,
                onValueChange = { cityText = it },
                label = { Text("Nama kota") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    cityMsg = "Mencari…"
                    vm.changeCity(cityText) { ok ->
                        cityMsg = if (ok) "Kota diperbarui" else "Kota tidak ditemukan / tidak ada internet"
                    }
                }) { Text("Cari & simpan") }
                Text(cityMsg, color = p.sheetSub, fontSize = 12.sp)
            }

            SectionTitle("Widget foto")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onClose(); onPickPhoto() }) { Text("Pilih foto") }
                OutlinedButton(onClick = { vm.clearPhoto() }) { Text("Hapus foto") }
            }
            Text("Tahan widget foto di layar utama untuk menghapusnya.", color = p.sheetSub, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))

            SectionTitle("Sistem")
            SwitchRow(
                "Status bar ala iOS",
                "Sembunyikan status bar Android di launcher dan tampilkan jam, sinyal, Wi-Fi, baterai gaya iOS",
                s.hideSystemStatusBar,
            ) { c -> vm.updateSettings { it.copy(hideSystemStatusBar = c) } }
            SwitchRow(
                "Pop up volume vertikal",
                "Ambil alih tombol volume saat launcher terbuka",
                s.customVolumeHud,
            ) { c -> vm.updateSettings { it.copy(customVolumeHud = c) } }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { vm.openWallpaperPicker() }) { Text("Ganti wallpaper") }
                OutlinedButton(onClick = { vm.openSettings(Settings.ACTION_HOME_SETTINGS) }) { Text("Launcher default") }
            }

            SectionTitle("Tentang")
            Text("Aeris Launcher v1.0.0", color = p.sheetText, fontSize = 14.sp)
            Text("Min. Android 8.0 (API 26)", color = p.sheetSub, fontSize = 12.sp)
            Spacer(Modifier.height(24.dp))
        }
    }
}
