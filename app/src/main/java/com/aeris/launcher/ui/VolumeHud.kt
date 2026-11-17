package com.aeris.launcher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeris.launcher.LauncherViewModel
import kotlinx.coroutines.delay

/** Pop up volume vertikal di sisi kanan. Muncul saat tombol volume ditekan, bisa digeser dengan jari. */
@Composable
fun VolumeHud(vm: LauncherViewModel, modifier: Modifier) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(vm.hudTick) {
        if (vm.hudTick > 0) {
            visible = true
            delay(1800)
            visible = false
        }
    }
    val shape = RoundedCornerShape(28.dp)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it }),
    ) {
        Column(
            Modifier
                .clip(shape)
                .background(Color(0xCC1C1C1E))
                .border(1.dp, Color(0x33FFFFFF), shape)
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "${(vm.mediaVolume * 100).toInt()}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            val icon = when {
                vm.mediaVolume <= 0f -> "🔇"
                vm.mediaVolume < 0.34f -> "🔈"
                vm.mediaVolume < 0.67f -> "🔉"
                else -> "🔊"
            }
            VerticalSlider(
                value = vm.mediaVolume,
                onValueChange = { vm.setMediaVolume(it); vm.touchHud() },
                icon = icon,
                modifier = Modifier.width(46.dp).height(170.dp),
                trackColor = Color(0x33FFFFFF),
            )
        }
    }
}
