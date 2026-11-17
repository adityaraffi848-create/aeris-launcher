package com.aeris.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.aeris.launcher.LauncherViewModel
import com.aeris.launcher.data.AppItem
import com.aeris.launcher.data.ThemeMode
import com.aeris.launcher.system.rememberSystemStatus
import kotlinx.coroutines.launch

@Composable
fun LauncherScreen(vm: LauncherViewModel, statusBarDp: Float) {
    val settings = vm.settings
    val palette = remember(settings.theme) { paletteFor(settings.theme) }
    val status = rememberSystemStatus()
    val barHeight = statusBarDp.dp

    var showCC by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var openCategory by remember { mutableStateOf<String?>(null) }
    var iconTarget by remember { mutableStateOf<AppItem?>(null) }

    val iconPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val t = iconTarget
        if (uri != null && t != null) vm.setCustomIcon(t, uri)
        iconTarget = null
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.setPhoto(uri)
    }
    val actions = remember(vm) {
        AppActions(
            vm = vm,
            pickIcon = { app -> iconTarget = app; iconPicker.launch("image/*") },
            openAerisSettings = { showSettings = true },
        )
    }

    val byKey = remember(vm.apps) { vm.apps.associateBy { it.key } }
    val homeApps = vm.homeKeys.mapNotNull { byKey[it] }
    val dockApps = vm.dockKeys.mapNotNull { byKey[it] }
    val pages = remember(homeApps) { listOf(homeApps.take(12)) + homeApps.drop(12).chunked(20) }
    val pageCount = pages.size + 1                       // halaman terakhir = Pustaka Aplikasi
    val pageCountState = rememberUpdatedState(pageCount)
    val pagerState = rememberPagerState { pageCountState.value }
    val scope = rememberCoroutineScope()
    val onHomePage = pagerState.currentPage < pageCount - 1
    val dockAlpha by animateFloatAsState(if (onHomePage) 1f else 0f, label = "dockAlpha")

    // tombol Home ditekan: tutup semua panel dan balik ke halaman pertama
    LaunchedEffect(vm.homeSignal) {
        if (vm.homeSignal > 0) {
            showCC = false; showSettings = false; showSearch = false; openCategory = null
            pagerState.animateScrollToPage(0)
        }
    }

    BackHandler(enabled = showCC || showSettings || showSearch || openCategory != null || pagerState.currentPage != 0) {
        when {
            showSearch -> showSearch = false
            showSettings -> showSettings = false
            showCC -> showCC = false
            openCategory != null -> openCategory = null
            else -> scope.launch { pagerState.animateScrollToPage(0) }
        }
    }

    MaterialTheme(colorScheme = if (settings.theme == ThemeMode.NORMAL) lightColorScheme() else darkColorScheme()) {
        CompositionLocalProvider(LocalPalette provides palette, LocalSettings provides settings) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(palette.scrim)
                    // geser ke bawah dari bagian atas layar = Pusat Kontrol
                    .pointerInput(Unit) {
                        var startY = 0f
                        var dy = 0f
                        detectVerticalDragGestures(
                            onDragStart = { startY = it.y; dy = 0f },
                            onDragEnd = { if (startY < size.height * 0.22f && dy > 60f) showCC = true },
                            onVerticalDrag = { _, d -> dy += d },
                        )
                    },
            ) {
                Column(Modifier.fillMaxSize()) {
                    if (settings.hideSystemStatusBar) {
                        IosStatusBar(status, barHeight) { showCC = true }
                    } else {
                        Spacer(Modifier.height(barHeight))
                    }

                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                            if (page < pages.size) {
                                HomePage(page, pages[page], actions, vm) { photoPicker.launch("image/*") }
                            } else {
                                AppLibrary(
                                    apps = vm.apps,
                                    actions = actions,
                                    onSearch = { showSearch = true },
                                    onOpenCategory = { openCategory = it },
                                )
                            }
                        }
                    }

                    Column(
                        Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PageDots(pageCount, pagerState.currentPage)
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.height(132.dp), contentAlignment = Alignment.BottomCenter) {
                            if (dockAlpha > 0.02f) {
                                Column(
                                    Modifier.graphicsLayer { alpha = dockAlpha },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    SearchPill(onSearch = { showSearch = true }, onSettings = { showSettings = true })
                                    Dock(dockApps, actions)
                                }
                            }
                        }
                    }
                }

                if (settings.customVolumeHud) {
                    VolumeHud(vm, Modifier.align(Alignment.CenterEnd).padding(end = 10.dp))
                }

                AnimatedVisibility(showCC, enter = fadeIn(), exit = fadeOut()) {
                    ControlCenter(
                        vm = vm,
                        status = status,
                        topPad = barHeight,
                        onClose = { showCC = false },
                        onOpenAeris = { showSettings = true },
                    )
                }

                openCategory?.let { cat ->
                    CategorySheet(
                        title = cat,
                        apps = vm.apps.filter { it.category == cat },
                        actions = actions,
                        onClose = { openCategory = null },
                    )
                }

                if (showSearch) SearchOverlay(vm.apps, vm) { showSearch = false }
                if (showSettings) SettingsSheet(vm, { photoPicker.launch("image/*") }) { showSettings = false }
            }
        }
    }
}

@Composable
private fun HomePage(
    index: Int,
    apps: List<AppItem>,
    actions: AppActions,
    vm: LauncherViewModel,
    onPickPhoto: () -> Unit,
) {
    val s = LocalSettings.current
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        val widgetBlock = if (index == 0) 166.dp else 0.dp
        val rows = if (index == 0) 3 else 5
        val rowH = ((maxHeight - widgetBlock) / rows).coerceIn(70.dp, 104.dp)
        Column(Modifier.fillMaxSize()) {
            if (index == 0) {
                Row(
                    Modifier.fillMaxWidth().height(150.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    WeatherWidget(vm.weather, Modifier.weight(1f).fillMaxHeight()) { vm.refreshWeather(true) }
                    PhotoWidget(vm, Modifier.weight(1f).fillMaxHeight(), onPickPhoto)
                }
                Spacer(Modifier.height(16.dp))
            }
            apps.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().height(rowH)) {
                    repeat(4) { i ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                            row.getOrNull(i)?.let { AppIconCell(it, actions, showLabel = s.showLabels) }
                        }
                    }
                }
            }
        }
    }
}
