package com.aismartmath.board.board

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.model.Tool
import com.aismartmath.board.model.Tools
import com.aismartmath.board.model.Topic
import com.aismartmath.board.ui.components.glass
import com.aismartmath.board.ui.theme.BoardColors
import kotlinx.coroutines.delay

/**
 * The tool bar: icons only, never scrolls. Tools that do not fit go into a "more" grid that opens with a pop.
 * The selected tool always stays visible. Long-press a tool for its name and hint.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolBar(state: BoardState, topic: Topic) {
    var open by rememberSaveable { mutableStateOf(false) }
    val accent = BoardColors.Indigo
    val shape = RoundedCornerShape(12.dp)
    // tapping the pen opens its colour / width box (tap again to close); the highlighter opens it on a double tap
    val pick: (String) -> Unit = { id ->
        if (id == state.tool) {
            when (id) {
                "pen" -> state.penOpts = !state.penOpts
                "er" -> state.eraserOpts = !state.eraserOpts
                else -> {}
            }
        } else {
            state.selectTool(id)
            if (id == "pen") state.penOpts = true
            if (id == "er") state.eraserOpts = true
        }
        open = false
    }
    val hlDouble: () -> Unit = {
        if (state.tool != "hl") state.selectTool("hl")
        state.penOpts = !state.penOpts
        open = false
    }
    val double: (String) -> (() -> Unit)? = { id -> if (id == "hl") hlDouble else null }
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val fit = ((maxWidth - 16.dp) / 32.dp).toInt().coerceAtLeast(3)
            val tools = topic.toolIds
            val all = tools.size <= fit
            val shown: List<String> = if (all) {
                tools
            } else {
                val base = tools.take(fit - 1).toMutableList()
                if (state.tool !in base) base[base.lastIndex] = state.tool
                base
            }
            Row(
                Modifier.align(Alignment.CenterStart).glass(shape, accent).padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                shown.forEachIndexed { i, id ->
                    val tool = Tools.byId.getValue(id)
                    ToolIcon(tool, state.tool == id, i, accent, { state.showToast(tool.icon + "  " + tool.name + " — " + tool.hint) }, double(id)) {
                        pick(id)
                    }
                }
                if (!all) MoreButton(open, accent) { open = !open }
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = open,
            enter = expandVertically(spring(dampingRatio = 0.7f, stiffness = 300f)) + fadeIn(tween(200)),
            exit = shrinkVertically(tween(160)) + fadeOut(tween(120)),
        ) {
            FlowRow(
                Modifier.padding(top = 6.dp).glass(shape, accent).padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                topic.toolIds.forEachIndexed { i, id ->
                    ToolCell(Tools.byId.getValue(id), state.tool == id, i, accent, double(id)) {
                        pick(id)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolIcon(tool: Tool, on: Boolean, index: Int, accent: Color, onLong: () -> Unit, onDouble: (() -> Unit)?, onClick: () -> Unit) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(60L + index * 40L)
        enter.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 240f))
    }
    val haptic = LocalHapticFeedback.current
    val clickNow by rememberUpdatedState(onClick)
    val longNow by rememberUpdatedState(onLong)
    val doubleNow by rememberUpdatedState(onDouble)
    var pressed by remember { mutableStateOf(false) }
    val bounce by animateFloatAsState(if (pressed) 0.88f else if (on) 1.08f else 1f, spring(dampingRatio = 0.5f, stiffness = 400f), label = "toolBounce")
    val bg by animateColorAsState(if (pressed) accent else if (on) accent.copy(alpha = 0.55f) else Color(0xFF3B271B), tween(140), label = "toolBg")
    val bd by animateColorAsState(if (on || pressed) Color(0xFFF1D3A5) else Color(0xFF6B4630), tween(140), label = "toolBd")
    val shape = RoundedCornerShape(9.dp)
    Box(
        Modifier
            .size(28.dp)
            .graphicsLayer {
                val e = enter.value
                alpha = e.coerceIn(0f, 1f)
                translationX = (1f - e) * -40f
                scaleX = bounce
                scaleY = bounce
            }
            .shadow(if (on && !pressed) 7.dp else if (pressed) 1.dp else 4.dp, shape, clip = false, ambientColor = if (on) accent else Color.Black, spotColor = if (on) accent else Color.Black)
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.dp, bd), shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        clickNow()
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        longNow()
                    },
                    onDoubleTap = if (onDouble == null) null else fun(_: Offset) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        doubleNow?.invoke()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) { Text(tool.icon, fontSize = 14.sp, color = Color.White) }
}

@Composable
private fun MoreButton(open: Boolean, accent: Color, onClick: () -> Unit) {
    val rot by animateFloatAsState(if (open) 180f else 0f, spring(dampingRatio = 0.6f), label = "moreRot")
    val shape = RoundedCornerShape(9.dp)
    val haptic = LocalHapticFeedback.current
    val clickNow by rememberUpdatedState(onClick)
    val fill by animateColorAsState(if (open) accent.copy(alpha = 0.7f) else Color(0xFF4A3022), tween(160), label = "moreFill")
    Box(
        Modifier
            .size(28.dp)
            .shadow(if (open) 1.dp else 4.dp, shape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(shape)
            .background(fill)
            .border(BorderStroke(1.dp, if (open) Color(0xFFF1D3A5) else Color(0xFF6B4630)), shape)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    clickNow()
                })
            },
        contentAlignment = Alignment.Center,
    ) { Text("▾", fontSize = 13.sp, color = Color.White, modifier = Modifier.graphicsLayer { rotationZ = rot }) }
}

/** One tool in the "more" grid: its icon with a tiny name under it. */
@Composable
private fun ToolCell(tool: Tool, on: Boolean, index: Int, accent: Color, onDouble: (() -> Unit)?, onClick: () -> Unit) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 25L)
        enter.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 260f))
    }
    val haptic = LocalHapticFeedback.current
    val clickNow by rememberUpdatedState(onClick)
    var pressed by remember { mutableStateOf(false) }
    val bg by animateColorAsState(if (pressed) accent else if (on) accent.copy(alpha = 0.55f) else Color(0xFF3B271B), tween(140), label = "cellBg")
    val shape = RoundedCornerShape(9.dp)
    Column(
        Modifier
            .width(42.dp)
            .graphicsLayer {
                val e = enter.value
                alpha = e.coerceIn(0f, 1f)
                translationY = (1f - e) * -24f
                val s = if (pressed) 0.92f else 1f
                scaleX = s
                scaleY = s
            }
            .shadow(if (pressed) 1.dp else 4.dp, shape, clip = false, ambientColor = if (on) accent else Color.Black, spotColor = if (on) accent else Color.Black)
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.dp, if (on || pressed) Color(0xFFF1D3A5) else Color(0xFF6B4630)), shape)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                }, onDoubleTap = if (onDouble == null) null else fun(_: Offset) { onDouble() }, onTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    clickNow()
                })
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(tool.icon, fontSize = 13.sp, color = Color.White)
        Text(tool.name, fontSize = 7.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1)
    }
}

/** A small glass tag in the corner of the board: the tool you are holding. */
@Composable
fun ToolBadge(toolId: String) {
    val tool = Tools.byId[toolId] ?: return
    AnimatedContent(
        targetState = tool,
        transitionSpec = {
            (slideInVertically(tween(220)) { -it } + fadeIn(tween(220))) togetherWith
                (slideOutVertically(tween(160)) { it } + fadeOut(tween(120)))
        },
        label = "badge",
    ) { t ->
        Row(
            Modifier.glass(RoundedCornerShape(9.dp), BoardColors.Indigo, 6.dp).padding(horizontal = 7.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(t.icon, fontSize = 10.sp, color = Color.White)
            Text(t.name, fontSize = 9.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Medium)
        }
    }
}

/** Soft coloured light drifting behind the board - blurred on Android 12 and newer, a soft gradient before that. */
@Composable
fun AmbientGlow(color: Color) {
    val c by animateColorAsState(color, tween(600), label = "ambColor")
    val t = rememberInfiniteTransition(label = "amb")
    val drift by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "drift",
    )
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-60).dp, y = (-40).dp)
                .size(240.dp)
                .graphicsLayer { translationX = drift * 90f; translationY = drift * 40f }
                .blur(60.dp)
                .background(Brush.radialGradient(listOf(c.copy(alpha = 0.40f), Color.Transparent))),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 70.dp, y = 60.dp)
                .size(260.dp)
                .graphicsLayer { translationX = -drift * 80f }
                .blur(70.dp)
                .background(Brush.radialGradient(listOf(Color(0xFFB45309).copy(alpha = 0.22f), Color.Transparent))),
        )
    }
}
