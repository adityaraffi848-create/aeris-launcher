package com.aeris.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aeris.launcher.data.AppItem
import com.aeris.launcher.data.CATEGORY_ORDER

/** Pustaka Aplikasi ala iOS: semua aplikasi dikelompokkan otomatis per kategori. */
@Composable
fun AppLibrary(
    apps: List<AppItem>,
    actions: AppActions,
    onSearch: () -> Unit,
    onOpenCategory: (String) -> Unit,
) {
    val p = LocalPalette.current
    val groups = remember(apps) { apps.groupBy { it.category } }
    val cats = CATEGORY_ORDER.filter { groups[it]?.isNotEmpty() == true }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .glass(p, RoundedCornerShape(16.dp))
                .pointerInput(Unit) { detectTapGestures { onSearch() } }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MagnifierIcon(p.onCard.copy(alpha = 0.75f), 15.dp)
            Text("Pustaka Aplikasi", color = p.onCard.copy(alpha = 0.75f), fontSize = 15.sp)
        }
        Spacer(Modifier.size(16.dp))
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            items(cats.chunked(2)) { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    pair.forEach { cat ->
                        LibraryCard(cat, groups[cat].orEmpty(), actions, onOpenCategory, Modifier.weight(1f))
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LibraryCard(
    category: String,
    apps: List<AppItem>,
    actions: AppActions,
    onOpen: (String) -> Unit,
    modifier: Modifier,
) {
    val p = LocalPalette.current
    val shown = apps.take(4)
    Column(modifier, horizontalAlignment = Alignment.Start) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .glass(p, RoundedCornerShape(26.dp))
                .pointerInput(category) { detectTapGestures { onOpen(category) } }
                .padding(10.dp),
        ) {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                for (r in 0..1) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (c in 0..1) {
                            Box(Modifier.size(46.dp)) {
                                shown.getOrNull(r * 2 + c)?.let {
                                    AppIconCell(it, actions, iconSize = 46.dp, showLabel = false, applyScale = false)
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            category,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            style = TextStyle(shadow = LabelShadow),
            modifier = Modifier.padding(start = 6.dp, top = 6.dp),
        )
    }
}

/** Lembar berisi semua aplikasi dari satu kategori. */
@Composable
fun CategorySheet(title: String, apps: List<AppItem>, actions: AppActions, onClose: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(p.sheetBg)
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(16.dp),
        ) {
            Text(title, color = p.sheetText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                apps.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(4) { i ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                                row.getOrNull(i)?.let {
                                    AppIconCell(it, actions, iconSize = 52.dp, applyScale = false, labelColor = p.sheetText, onLaunched = onClose)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
