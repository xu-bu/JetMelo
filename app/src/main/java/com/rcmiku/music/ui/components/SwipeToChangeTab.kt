package com.rcmiku.music.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Swipe left to go to the next tab, swipe right to go back to the previous one.
 * Horizontal scrollables inside the content (rows, carousels) consume their own drags first,
 * so they keep working; vertical scrolling is unaffected.
 */
fun Modifier.swipeToChangeTab(
    selectedIndex: Int,
    tabCount: Int,
    onSelected: (Int) -> Unit
): Modifier = pointerInput(selectedIndex, tabCount) {
    val threshold = 72.dp.toPx()
    var total = 0f
    detectHorizontalDragGestures(
        onDragStart = { total = 0f },
        onDragCancel = { total = 0f },
        onDragEnd = {
            when {
                total <= -threshold && selectedIndex < tabCount - 1 -> onSelected(selectedIndex + 1)
                total >= threshold && selectedIndex > 0 -> onSelected(selectedIndex - 1)
            }
            total = 0f
        },
        onHorizontalDrag = { _, dragAmount -> total += dragAmount }
    )
}
