package com.aeris.launcher.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeris.launcher.LauncherViewModel
import com.aeris.launcher.data.WeatherData
import com.aeris.launcher.data.WeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun weatherColors(code: Int, day: Boolean): List<Color> = when {
    !day -> listOf(Color(0xFF1B2A4E), Color(0xFF3C4F8A))
    code >= 95 -> listOf(Color(0xFF3B3F57), Color(0xFF6A5F86))
    code in 51..67 || code in 80..82 -> listOf(Color(0xFF4B5C74), Color(0xFF7B8FA8))
    code == 3 || code in 45..48 -> listOf(Color(0xFF5D7A9C), Color(0xFF8FA9C4))
    else -> listOf(Color(0xFF2F80ED), Color(0xFF56CCF2))
}

/** Widget cuaca bertema iOS. Ketuk untuk menyegarkan. */
@Composable
fun WeatherWidget(w: WeatherData?, modifier: Modifier, onClick: () -> Unit) {
    val colors = if (w == null) weatherColors(0, true) else weatherColors(w.code, w.isDay)
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier
            .clip(shape)
            .background(Brush.verticalGradient(colors))
            .pointerInput(Unit) { detectTapGestures { onClick() } }
            .padding(13.dp),
    ) {
        if (w == null) {
            Text("Memuat cuaca…", color = Color.White, fontSize = 13.sp, modifier = Modifier.align(Alignment.Center))
        } else {
            Column(Modifier.fillMaxSize()) {
                Text(w.city, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${w.temp}°", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.weight(1f))
                Text(WeatherRepository.emoji(w.code, w.isDay), fontSize = 17.sp)
                Text(WeatherRepository.describe(w.code), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text("↑${w.max}°  ↓${w.min}°", color = Color(0xCCFFFFFF), fontSize = 12.sp)
            }
        }
    }
}

/** Widget foto. Ketuk = pilih foto, tahan = hapus foto. */
@Composable
fun PhotoWidget(vm: LauncherViewModel, modifier: Modifier, onPick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(26.dp)
    val bmp by produceState<ImageBitmap?>(null, vm.photoVersion) {
        value = withContext(Dispatchers.IO) {
            val f = vm.photoFile()
            if (f.exists()) BitmapFactory.decodeFile(f.path)?.asImageBitmap() else null
        }
    }
    Box(
        modifier
            .clip(shape)
            .background(p.cardBrush)
            .border(1.dp, p.cardBorder, shape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onPick() }, onLongPress = { vm.clearPhoto() })
            },
        contentAlignment = Alignment.Center,
    ) {
        val img = bmp
        if (img != null) {
            Image(img, contentDescription = "Foto", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🖼️", fontSize = 26.sp)
                Text("Ketuk untuk\npilih foto", color = p.onCard, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
    }
}
