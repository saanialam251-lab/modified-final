package com.aismartmath.board.ui.boards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.data.BoardMeta
import com.aismartmath.board.data.BoardStore
import com.aismartmath.board.model.Topics
import com.aismartmath.board.ui.components.IconBtn
import com.aismartmath.board.ui.components.slab
import com.aismartmath.board.ui.components.RenameDialog
import com.aismartmath.board.ui.theme.BoardColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The saved boards: open, rename, duplicate or delete them. */
@Composable
fun BoardsScreen(store: BoardStore, onOpen: (BoardMeta) -> Unit, onClose: () -> Unit) {
    var tick by remember { mutableIntStateOf(0) }
    val boards = remember(tick) { store.list() }
    var renaming by remember { mutableStateOf<BoardMeta?>(null) }
    var deleting by remember { mutableStateOf<BoardMeta?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(BoardColors.Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconBtn("←") { onClose() }
            Text("📂 My boards", color = Color(0xFFF1D3A5), fontSize = 19.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold)
        }
        if (boards.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No saved boards yet.\nOpen a topic and draw — boards save by themselves.",
                    color = BoardColors.Muted,
                    fontSize = 15.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(boards, key = { it.id }) { b ->
                    BoardRow(
                        b,
                        onOpen = { onOpen(b) },
                        onRename = { renaming = b },
                        onCopy = {
                            store.duplicate(b.id)
                            tick++
                        },
                        onDelete = { deleting = b },
                    )
                }
            }
        }
    }

    renaming?.let { b ->
        RenameDialog(
            initial = b.name,
            onDismiss = { renaming = null },
            onSave = { name ->
                store.rename(b.id, name)
                renaming = null
                tick++
            },
        )
    }
    deleting?.let { b ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete this board?") },
            text = { Text("“${b.name}” will be removed from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        store.delete(b.id)
                        deleting = null
                        tick++
                    },
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun BoardRow(b: BoardMeta, onOpen: () -> Unit, onRename: () -> Unit, onCopy: () -> Unit, onDelete: () -> Unit) {
    val topic = Topics.byId[b.topicId]
    val date = remember(b.updated) { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(b.updated)) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 700.dp)
            .slab(shape, Color(0xFF2E1E15), Color(0xFF5C3D28), 8.dp)
            .clickable(onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier.size(40.dp).slab(RoundedCornerShape(10.dp), topic?.color ?: BoardColors.Indigo, Color(0xFF5C3D28), 4.dp),
            contentAlignment = Alignment.Center,
        ) { Text(topic?.icon ?: "▦", fontSize = 22.sp, color = Color.White) }
        Column(Modifier.weight(1f)) {
            Text(b.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                (topic?.name ?: "Board") + " · " + b.count + " figures · " + date,
                color = BoardColors.Muted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconBtn("✎", compact = true) { onRename() }
        IconBtn("⧉", compact = true) { onCopy() }
        IconBtn("🗑", compact = true) { onDelete() }
    }
}
