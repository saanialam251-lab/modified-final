package com.aismartmath.board.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aismartmath.board.ui.components.Chip
import com.aismartmath.board.ui.components.glass
import com.aismartmath.board.ui.theme.BoardColors

/**
 * The two faces of the Sketch tool:
 *  - Shape: draw roughly and it becomes a neat figure (what it always did);
 *  - Drag: drag a box round writing, drawings or pictures to select them, then copy, cut, paste, move or resize them.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SketchPanel(state: BoardState) {
    val acc = BoardColors.Indigo
    FlowRow(
        Modifier.fillMaxWidth().glass(RoundedCornerShape(12.dp), acc, 8.dp).padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Chip("⬚ Drag", state.skDrag, acc, onLong = { state.showToast("Drag a box round things to select them") }, small = true) {
            state.skDrag = true
            state.showToast("Drag a box round what you want to select")
        }
        Chip("✏ Shape", !state.skDrag, acc, onLong = { state.showToast("Draw roughly — it becomes a neat figure") }, small = true) {
            state.skDrag = false
            state.clearGroup()
        }
        if (state.skDrag) {
            if (state.group.isNotEmpty()) {
                Chip("📋", false, acc, onLong = { state.showToast("Copy") }, small = true) { state.copyGroup(false) }
                Chip("✂️", false, acc, onLong = { state.showToast("Cut") }, small = true) { state.copyGroup(true) }
                Chip("🗑️", false, acc, onLong = { state.showToast("Delete") }, small = true) { state.deleteGroup() }
                Chip("✕", false, acc, onLong = { state.showToast("Un-select") }, small = true) { state.clearGroup() }
            }
            Chip("📥", false, acc, onLong = { state.showToast("Paste text, a picture, or what you copied") }, small = true) { state.pasteHere(null) }
            Chip("🖼️", false, acc, onLong = { state.showToast("Add a picture from your phone") }, small = true) { state.pickImage() }
        }
    }
}
