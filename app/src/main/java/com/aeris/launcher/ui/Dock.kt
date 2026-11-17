package com.aeris.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeris.launcher.data.AppItem

/** Dock kaca dengan 4 slot aplikasi. */
@Composable
fun Dock(apps: List<AppItem>, actions: AppActions) {
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(86.dp)
            .glass(p, RoundedCornerShape(32.dp), dock = true)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(4) { i ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                apps.getOrNull(i)?.let { AppIconCell(it, actions, showLabel = false) }
            }
        }
    }
}

/** Tombol kecil "Cari" di atas dock. Ketuk = cari, tahan = Pengaturan Aeris. */
@Composable
fun SearchPill(onSearch: () -> Unit, onSettings: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier
            .glass(p, CircleShape)
            .pointerInput(Unit) { detectTapGestures(onTap = { onSearch() }, onLongPress = { onSettings() }) }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        MagnifierIcon(p.onCard, 13.dp)
        Text("Cari", color = p.onCard, fontSize = 13.sp)
    }
}

@Composable
fun PageDots(count: Int, current: Int) {
    Row(
        Modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .size(if (i == current) 7.dp else 6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (i == current) 0.95f else 0.4f)),
            )
        }
    }
}
