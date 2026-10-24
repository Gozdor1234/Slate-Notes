package com.gozdor.slatenotes.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gozdor.slatenotes.data.CEdge
import com.gozdor.slatenotes.data.CNode
import com.gozdor.slatenotes.data.CanvasFile
import com.gozdor.slatenotes.data.Fuzzy
import com.gozdor.slatenotes.data.Md
import com.gozdor.slatenotes.ui.AppViewModel
import com.gozdor.slatenotes.ui.editor.HighlightColors
import com.gozdor.slatenotes.ui.editor.MarkdownHighlighter
import com.gozdor.slatenotes.ui.theme.Lucide
import com.gozdor.slatenotes.ui.workspace.CircleIconButton
import com.gozdor.slatenotes.ui.workspace.NavIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val imageExt = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp")

/** JSON Canvas leaves preset colors up to the app. These are Slate's choices for 1 to 6. */
fun canvasColor(c: String?, fallback: Color): Color = when (c) {
    null, "" -> fallback
    "1" -> Color(0xFFE93147)
    "2" -> Color(0xFFEC7500)
    "3" -> Color(0xFFE0AC00)
    "4" -> Color(0xFF08B94E)
    "5" -> Color(0xFF00BFBC)
    "6" -> Color(0xFFA882FF)
    else -> parseHexColor(c) ?: fallback
}

/** CSS-style hex: "#RGB", "#RRGGBB" or "#RRGGBBAA". */
fun parseHexColor(s: String): Color? {
    val h = s.removePrefix("#")
    val v = h.toLongOrNull(16) ?: return null
    return when (h.length) {
        3 -> {
            val r = (v shr 8) and 0xF
            val g = (v shr 4) and 0xF
            val b = v and 0xF
            Color((0xFF000000 or (r * 17 shl 16) or (g * 17 shl 8) or (b * 17)).toInt())
        }
        6 -> Color((0xFF000000 or v).toInt())
        8 -> Color((((v and 0xFF) shl 24) or (v shr 8)).toInt())
        else -> null
    }
}

private val colorChoices = listOf(null to "No color", "1" to "Red", "2" to "Orange", "3" to "Yellow", "4" to "Green", "5" to "Cyan", "6" to "Purple")

private fun side(n: CNode, s: String): Offset = when (s) {
    "top" -> Offset(n.x + n.w / 2, n.y)
    "bottom" -> Offset(n.x + n.w / 2, n.y + n.h)
    "left" -> Offset(n.x, n.y + n.h / 2)
    else -> Offset(n.x + n.w, n.y + n.h / 2)
}

private fun normal(s: String): Offset = when (s) {
    "top" -> Offset(0f, -1f)
    "bottom" -> Offset(0f, 1f)
    "left" -> Offset(-1f, 0f)
    else -> Offset(1f, 0f)
}

/** Picks facing sides when an edge doesn't say which sides it uses. */
private fun autoSides(a: CNode, b: CNode): Pair<String, String> {
    val dx = (b.x + b.w / 2) - (a.x + a.w / 2)
    val dy = (b.y + b.h / 2) - (a.y + a.h / 2)
    return if (abs(dx) > abs(dy)) {
        if (dx > 0) "right" to "left" else "left" to "right"
    } else {
        if (dy > 0) "bottom" to "top" else "top" to "bottom"
    }
}

/** Cubic curve control points for an edge, in canvas units. */
private fun curve(e: CEdge, a: CNode, b: CNode): List<Offset> {
    val auto = autoSides(a, b)
    val fs = e.fromSide ?: auto.first
    val ts = e.toSide ?: auto.second
    val p0 = side(a, fs)
    val p3 = side(b, ts)
    val k = max(40f, (p3 - p0).getDistance() * 0.4f)
    return listOf(p0, p0 + normal(fs) * k, p3 + normal(ts) * k, p3)
}

private fun bezier(p: List<Offset>, t: Float): Offset {
    val u = 1 - t
    return p[0] * (u * u * u) + p[1] * (3 * u * u * t) + p[2] * (3 * u * t * t) + p[3] * (t * t * t)
}

@Composable
fun CanvasView(vm: AppViewModel, path: String, controlsBottomPadding: Dp = 96.dp) {
    val file = remember(path) { File(path) }
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current.density
    val textMeasurer = rememberTextMeasurer()

    var root by remember(path) { mutableStateOf(JSONObject()) }
    val nodes = remember(path) { mutableStateListOf<CNode>() }
    val edges = remember(path) { mutableStateListOf<CEdge>() }
    var loaded by remember(path) { mutableStateOf(false) }
    var error by remember(path) { mutableStateOf<String?>(null) }
    var changes by remember(path) { mutableIntStateOf(0) }
    var savedAt by remember(path) { mutableIntStateOf(0) }

    var pan by remember(path) { mutableStateOf(Offset.Zero) }
    var scale by remember(path) { mutableFloatStateOf(1f) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var fitted by remember(path) { mutableStateOf(false) }

    var selNode by remember(path) { mutableStateOf<String?>(null) }
    var selEdge by remember(path) { mutableStateOf<String?>(null) }
    var connecting by remember(path) { mutableStateOf(false) }
    var editing by remember(path) { mutableStateOf<String?>(null) }
    var pickingNote by remember(path) { mutableStateOf(false) }
    var addMenu by remember { mutableStateOf(false) }
    var colorMenu by remember { mutableStateOf(false) }

    val highlighter = remember(cs) {
        MarkdownHighlighter(
            HighlightColors(
                accent = cs.primary,
                dim = cs.onSurfaceVariant.copy(alpha = 0.6f),
                codeBg = cs.surfaceVariant,
                highlightBg = cs.primary.copy(alpha = 0.25f),
            )
        )
    }

    LaunchedEffect(path) {
        val r = withContext(Dispatchers.IO) { runCatching { CanvasFile.parse(file.readText()) } }
        r.onSuccess { doc ->
            root = doc.root
            nodes.addAll(doc.nodes)
            edges.addAll(doc.edges)
            loaded = true
        }.onFailure { error = "This canvas couldn't be read (${it.message})." }
    }

    fun save() {
        vm.saveFile(path, CanvasFile.serialize(root, nodes.toList(), edges.toList()))
        savedAt = changes
    }

    LaunchedEffect(changes) {
        if (changes == savedAt) return@LaunchedEffect
        delay(500)
        save()
    }
    DisposableEffect(path) {
        onDispose { if (loaded && changes != savedAt) save() }
    }

    fun changed() {
        changes++
    }

    fun fit() {
        if (viewSize.width == 0) return
        if (nodes.isEmpty()) {
            scale = 1f
            pan = Offset(viewSize.width / 2f, viewSize.height / 2f)
            return
        }
        val minX = nodes.minOf { it.x }
        val maxX = nodes.maxOf { it.x + it.w }
        val minY = nodes.minOf { it.y }
        val maxY = nodes.maxOf { it.y + it.h }
        val s = min(
            viewSize.width * 0.9f / ((maxX - minX) * density),
            viewSize.height * 0.75f / ((maxY - minY) * density),
        ).coerceIn(0.1f, 1.5f)
        scale = s
        pan = Offset(
            viewSize.width / 2f - (minX + maxX) / 2 * density * s,
            viewSize.height / 2f - (minY + maxY) / 2 * density * s,
        )
    }

    LaunchedEffect(loaded, viewSize) {
        if (loaded && !fitted && viewSize.width > 0) {
            fit()
            fitted = true
        }
    }

    fun toCanvas(p: Offset): Offset = Offset((p.x - pan.x) / scale / density, (p.y - pan.y) / scale / density)

    fun nodeAt(c: Offset): CNode? {
        val hits = nodes.filter { c.x >= it.x && c.x <= it.x + it.w && c.y >= it.y && c.y <= it.y + it.h }
        return hits.lastOrNull { it.type != "group" } ?: hits.lastOrNull()
    }

    fun edgeAt(c: Offset): CEdge? {
        val byId = nodes.associateBy { it.id }
        val tolerance = 16f / scale
        return edges.lastOrNull { e ->
            val a = byId[e.from] ?: return@lastOrNull false
            val b = byId[e.to] ?: return@lastOrNull false
            val pts = curve(e, a, b)
            (0..24).any { i -> (bezier(pts, i / 24f) - c).getDistance() < tolerance }
        }
    }

    fun isInside(n: CNode, g: CNode) =
        n.x >= g.x && n.y >= g.y && n.x + n.w <= g.x + g.w && n.y + n.h <= g.y + g.h

    fun update(id: String, f: (CNode) -> CNode) {
        val i = nodes.indexOfFirst { it.id == id }
        if (i >= 0) nodes[i] = f(nodes[i])
    }

    fun deleteNode(id: String) {
        nodes.removeAll { it.id == id }
        edges.removeAll { it.from == id || it.to == id }
        selNode = null
        changed()
    }

    /** New cards go in the middle of what's on screen, nudged so they don't stack exactly. */
    fun addNode(type: String, w: Float, h: Float, text: String = "", fileRel: String = "", label: String = "") {
        val center = toCanvas(Offset(viewSize.width / 2f, viewSize.height / 2f))
        val nudge = (nodes.size % 5) * 24f
        val n = CNode(
            id = CanvasFile.newId(), type = type,
            x = center.x - w / 2 + nudge, y = center.y - h / 2 + nudge, w = w, h = h,
            color = null, text = text, file = fileRel, url = "", label = label, raw = JSONObject(),
        )
        // Groups go to the bottom of the stack so they sit behind cards.
        if (type == "group") nodes.add(0, n) else nodes.add(n)
        selNode = n.id
        selEdge = null
        changed()
        if (type == "text" || type == "group") editing = n.id
    }

    fun connect(fromId: String, toId: String) {
        val a = nodes.firstOrNull { it.id == fromId } ?: return
        val b = nodes.firstOrNull { it.id == toId } ?: return
        val (fs, ts) = autoSides(a, b)
        edges.add(CEdge(CanvasFile.newId(), fromId, toId, fs, ts, null, null, null, null, JSONObject()))
        changed()
    }

    fun openNode(n: CNode) {
        when (n.type) {
            "file" -> {
                val v = vm.vault ?: return
                val f = File(v.root, n.file)
                when {
                    !f.isFile -> vm.toast("\"${n.file}\" doesn't exist in this vault")
                    f.extension.equals("md", true) || f.extension.equals("canvas", true) -> vm.openNote(f)
                    else -> vm.toast("Can't open .${f.extension} files yet")
                }
            }
            "link" -> if (!vm.platform.openUrl(n.url)) vm.toast("No app can open this link")
        }
    }

    error?.let {
        Column(
            Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(it, color = cs.onSurfaceVariant)
            TextButton(onClick = { vm.closeTab(vm.activeTab) }) { Text("Close tab") }
        }
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(cs.background)
            .onSizeChanged { viewSize = it }
            .pointerInput(path, density) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = down.position
                    val c0 = toCanvas(start)
                    val sel = nodes.firstOrNull { it.id == selNode }
                    // The resize handle sits on the selected card's bottom-right corner.
                    val onHandle = sel != null && run {
                        val corner = Offset(
                            pan.x + (sel.x + sel.w) * density * scale,
                            pan.y + (sel.y + sel.h) * density * scale,
                        )
                        (start - corner).getDistance() < 28 * density
                    }
                    val hit = nodeAt(c0)
                    var mode = 0 // 0 = undecided, 1 = pan, 2 = move, 3 = resize, 4 = pinch
                    var members = emptyList<String>()
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            mode = 4
                            val zoom = event.calculateZoom()
                            val centroid = event.calculateCentroid(useCurrent = true)
                            val newScale = (scale * zoom).coerceIn(0.1f, 4f)
                            val z = newScale / scale
                            pan = (pan - centroid) * z + centroid + event.calculatePan()
                            scale = newScale
                            event.changes.forEach { it.consume() }
                        } else if (pressed.size == 1) {
                            val ch = pressed[0]
                            if (mode == 0 && (ch.position - start).getDistance() > viewConfiguration.touchSlop) {
                                mode = when {
                                    onHandle -> 3
                                    sel != null && hit?.id == sel.id -> 2
                                    else -> 1
                                }
                                if (mode == 2 && sel?.type == "group") {
                                    members = nodes.filter { it.id != sel.id && isInside(it, sel) }.map { it.id }
                                }
                            }
                            val d = ch.positionChange()
                            val dc = d / scale / density
                            when (mode) {
                                1 -> pan += d
                                2 -> (members + sel!!.id).forEach { id -> update(id) { it.copy(x = it.x + dc.x, y = it.y + dc.y) } }
                                3 -> update(sel!!.id) { it.copy(w = max(80f, it.w + dc.x), h = max(40f, it.h + dc.y)) }
                            }
                            if (mode != 0) ch.consume()
                        }
                    } while (event.changes.any { it.pressed })

                    when (mode) {
                        2, 3 -> changed()
                        0 -> {
                            // A tap.
                            if (connecting) {
                                val from = selNode
                                if (hit != null && from != null && hit.id != from) connect(from, hit.id)
                                connecting = false
                            } else if (hit != null) {
                                if (hit.id == selNode && (hit.type == "text" || hit.type == "group")) editing = hit.id
                                selNode = hit.id
                                selEdge = null
                            } else {
                                selEdge = edgeAt(c0)?.id
                                selNode = null
                            }
                        }
                    }
                }
            }
            // Mouse wheel: Ctrl+wheel zooms at the pointer, wheel pans (Shift+wheel pans sideways).
            .pointerInput(path, density) {
                awaitPointerEventScope {
                    while (true) {
                        val e = awaitPointerEvent()
                        if (e.type != PointerEventType.Scroll) continue
                        val ch = e.changes.firstOrNull() ?: continue
                        val d = ch.scrollDelta
                        if (e.keyboardModifiers.isCtrlPressed) {
                            val newScale = (scale * if (d.y > 0) 0.9f else 1.1f).coerceIn(0.1f, 4f)
                            val z = newScale / scale
                            pan = (pan - ch.position) * z + ch.position
                            scale = newScale
                        } else {
                            val step = 40f * density
                            pan -= if (e.keyboardModifiers.isShiftPressed) Offset(d.y * step, 0f) else Offset(d.x * step, d.y * step)
                        }
                        ch.consume()
                    }
                }
            },
    ) {
        // Dot grid that moves with the canvas.
        val dot = cs.outlineVariant
        Canvas(Modifier.fillMaxSize()) {
            val step = 24f * density * scale
            if (step >= 10f) {
                val ox = ((pan.x % step) + step) % step
                val oy = ((pan.y % step) + step) % step
                var x = ox
                while (x < size.width) {
                    var y = oy
                    while (y < size.height) {
                        drawCircle(dot, radius = 1.4f * density, center = Offset(x, y))
                        y += step
                    }
                    x += step
                }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = pan.x
                    translationY = pan.y
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        ) {
            for (n in nodes) if (n.type == "group") NodeCard(vm, n, n.id == selNode, highlighter, density)

            val edgeDefault = cs.onSurfaceVariant
            val labelStyle = TextStyle(fontSize = 13.sp, color = cs.onSurface)
            val labelBg = cs.background
            Canvas(Modifier.fillMaxSize()) {
                val byId = nodes.associateBy { it.id }
                for (e in edges) {
                    val a = byId[e.from] ?: continue
                    val b = byId[e.to] ?: continue
                    val pts = curve(e, a, b).map { it * density }
                    val selected = e.id == selEdge
                    val color = if (selected) cs.primary else canvasColor(e.color, edgeDefault)
                    val p = Path().apply {
                        moveTo(pts[0].x, pts[0].y)
                        cubicTo(pts[1].x, pts[1].y, pts[2].x, pts[2].y, pts[3].x, pts[3].y)
                    }
                    drawPath(p, color, style = Stroke(width = (if (selected) 3.5f else 2.5f) * density, cap = StrokeCap.Round))

                    fun arrow(tip: Offset, from: Offset) {
                        var dir = tip - from
                        val len = dir.getDistance()
                        if (len < 0.01f) return
                        dir /= len
                        val perp = Offset(-dir.y, dir.x)
                        val s = 12f * density
                        drawPath(
                            Path().apply {
                                moveTo(tip.x, tip.y)
                                val l = tip - dir * s + perp * (s * 0.55f)
                                val r = tip - dir * s - perp * (s * 0.55f)
                                lineTo(l.x, l.y)
                                lineTo(r.x, r.y)
                                close()
                            },
                            color,
                        )
                    }
                    // Per the spec, the end has an arrow unless it says "none"; the start has none unless it says "arrow".
                    if ((e.toEnd ?: "arrow") == "arrow") arrow(pts[3], pts[2])
                    if (e.fromEnd == "arrow") arrow(pts[0], pts[1])

                    val label = e.label
                    if (!label.isNullOrEmpty()) {
                        val mid = bezier(pts, 0.5f)
                        val layout = textMeasurer.measure(label, labelStyle)
                        val tl = mid - Offset(layout.size.width / 2f, layout.size.height / 2f)
                        drawRect(labelBg, topLeft = tl - Offset(6f, 2f), size = androidx.compose.ui.geometry.Size(layout.size.width + 12f, layout.size.height + 4f))
                        drawText(layout, topLeft = tl)
                    }
                }
            }

            for (n in nodes) if (n.type != "group") NodeCard(vm, n, n.id == selNode, highlighter, density)
        }

        // ---- floating controls
        if (connecting) {
            Surface(
                shape = RoundedCornerShape(50),
                color = cs.primaryContainer,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp),
            ) {
                Row(Modifier.padding(start = 18.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Tap a card to connect to", color = cs.onPrimaryContainer)
                    TextButton(onClick = { connecting = false }) { Text("Cancel") }
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = controlsBottomPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End,
        ) {
            CircleIconButton(Lucide.Maximize, "Zoom to fit") { fit() }
            Box {
                CircleIconButton(Lucide.Plus, "Add to canvas") { addMenu = true }
                DropdownMenu(
                    expanded = addMenu,
                    onDismissRequest = { addMenu = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = cs.surface,
                ) {
                    DropdownMenuItem(
                        text = { Text("Add card") },
                        leadingIcon = { Icon(Lucide.Type, null, Modifier.size(20.dp)) },
                        onClick = { addMenu = false; addNode("text", 250f, 60f) },
                    )
                    DropdownMenuItem(
                        text = { Text("Add note from vault") },
                        leadingIcon = { Icon(Lucide.FilePlus, null, Modifier.size(20.dp)) },
                        onClick = { addMenu = false; pickingNote = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Add group") },
                        leadingIcon = { Icon(Lucide.Group, null, Modifier.size(20.dp)) },
                        onClick = { addMenu = false; addNode("group", 500f, 360f, label = "Group") },
                    )
                }
            }
        }

        val selectedNode = nodes.firstOrNull { it.id == selNode }
        val selectedEdge = edges.firstOrNull { it.id == selEdge }
        if ((selectedNode != null || selectedEdge != null) && !connecting) {
            Surface(
                shape = RoundedCornerShape(50),
                color = cs.surface,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = controlsBottomPadding),
            ) {
                Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (selectedNode != null) {
                        if (selectedNode.type == "text" || selectedNode.type == "group" || selectedNode.type == "link") {
                            NavIcon(Lucide.Pencil, "Edit") { editing = selectedNode.id }
                        }
                        if (selectedNode.type == "file" || selectedNode.type == "link") {
                            NavIcon(Lucide.ExternalLink, "Open") { openNode(selectedNode) }
                        }
                        NavIcon(Lucide.Spline, "Connect to another card") { connecting = true }
                    }
                    Box {
                        NavIcon(Lucide.Palette, "Color") { colorMenu = true }
                        DropdownMenu(
                            expanded = colorMenu,
                            onDismissRequest = { colorMenu = false },
                            shape = RoundedCornerShape(16.dp),
                            containerColor = cs.surface,
                        ) {
                            colorChoices.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    leadingIcon = {
                                        Box(
                                            Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(canvasColor(code, cs.outlineVariant)),
                                        )
                                    },
                                    onClick = {
                                        colorMenu = false
                                        if (selectedNode != null) update(selectedNode.id) { it.copy(color = code) }
                                        if (selectedEdge != null) {
                                            val i = edges.indexOfFirst { it.id == selectedEdge.id }
                                            if (i >= 0) edges[i] = edges[i].copy(color = code)
                                        }
                                        changed()
                                    },
                                )
                            }
                        }
                    }
                    NavIcon(Lucide.Trash, "Delete") {
                        if (selectedNode != null) deleteNode(selectedNode.id)
                        if (selectedEdge != null) {
                            edges.removeAll { it.id == selectedEdge.id }
                            selEdge = null
                            changed()
                        }
                    }
                }
            }
        }
    }

    // ---- dialogs
    editing?.let { id ->
        val n = nodes.firstOrNull { it.id == id }
        if (n == null) {
            editing = null
            return@let
        }
        EditNodeDialog(n, onDismiss = { editing = null }) { value ->
            editing = null
            update(id) {
                when (it.type) {
                    "group" -> it.copy(label = value)
                    "link" -> it.copy(url = value)
                    else -> it.copy(text = value)
                }
            }
            changed()
        }
    }

    if (pickingNote) {
        NotePicker(vm, onDismiss = { pickingNote = false }) { f ->
            pickingNote = false
            val rel = vm.vault?.rel(f) ?: return@NotePicker
            addNode("file", 400f, 300f, fileRel = rel)
        }
    }
}

@Composable
private fun NodeCard(vm: AppViewModel, n: CNode, selected: Boolean, highlighter: MarkdownHighlighter, density: Float) {
    val cs = MaterialTheme.colorScheme
    val accent = canvasColor(n.color, cs.outline)
    val border = if (selected) cs.primary else accent
    Box(
        Modifier
            .offset { IntOffset((n.x * density).roundToInt(), (n.y * density).roundToInt()) }
            .size(n.w.dp, n.h.dp),
    ) {
        if (n.type == "group") {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = if (n.color == null) 0.05f else 0.1f))
                    .border(if (selected) 2.5.dp else 2.dp, border, RoundedCornerShape(14.dp)),
            )
            if (n.label.isNotEmpty()) {
                Text(
                    n.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(x = 4.dp, y = (-26).dp),
                )
            }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(cs.surface)
                    .background(if (n.color == null) Color.Transparent else accent.copy(alpha = 0.1f))
                    .border(if (selected) 2.5.dp else 1.5.dp, border, RoundedCornerShape(10.dp)),
            ) {
                when (n.type) {
                    "text" -> Text(
                        highlighter.highlight(n.text),
                        fontSize = 15.sp,
                        color = cs.onSurface,
                        modifier = Modifier.padding(12.dp),
                    )
                    "file" -> FileCardContent(vm, n, highlighter)
                    "link" -> Column(Modifier.padding(12.dp)) {
                        Icon(Lucide.Link, null, tint = cs.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(n.url, color = cs.primary, fontSize = 14.sp)
                    }
                    else -> Text("Unsupported card type: ${n.type}", color = cs.onSurfaceVariant, modifier = Modifier.padding(12.dp))
                }
            }
            if (selected) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(7.dp, 7.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(cs.primary),
                )
            }
        }
    }
}

@Composable
private fun FileCardContent(vm: AppViewModel, n: CNode, highlighter: MarkdownHighlighter) {
    val cs = MaterialTheme.colorScheme
    val v = vm.vault
    val f = remember(n.file, v) { v?.let { File(it.root, n.file) } }
    val ext = f?.extension?.lowercase().orEmpty()
    if (f != null && ext in imageExt) {
        val bmp by produceState<ImageBitmap?>(null, f) {
            value = withContext(Dispatchers.IO) { runCatching { vm.platform.loadImage(f, 1200) }.getOrNull() }
        }
        bmp?.let { Image(it, contentDescription = f.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
            ?: Text(f.name, modifier = Modifier.padding(12.dp), color = cs.onSurfaceVariant)
        return
    }
    val note = f?.let { vm.index.notes[it.absolutePath] }
    Column(Modifier.padding(12.dp)) {
        Text(
            f?.nameWithoutExtension ?: n.file,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        HorizontalDivider(color = cs.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
        when {
            note != null -> {
                val body = note.content.substring(Md.frontmatter(note.content)?.end ?: 0).trim()
                Text(highlighter.highlight(body), fontSize = 14.sp, color = cs.onSurface)
            }
            f == null || !f.exists() -> Text("File not found", color = cs.error, fontSize = 14.sp)
            else -> Text(f.name, color = cs.onSurfaceVariant, fontSize = 14.sp)
        }
    }
}

@Composable
private fun EditNodeDialog(n: CNode, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val initial = when (n.type) {
        "group" -> n.label
        "link" -> n.url
        else -> n.text
    }
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(when (n.type) { "group" -> "Group label"; "link" -> "Link"; else -> "Card" }) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = n.type != "text",
                minLines = if (n.type == "text") 6 else 1,
                placeholder = { Text(if (n.type == "text") "Write Markdown…" else "") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("Done") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun NotePicker(vm: AppViewModel, onDismiss: () -> Unit, onPick: (File) -> Unit) {
    var query by remember { mutableStateOf("") }
    val cs = MaterialTheme.colorScheme
    val all = remember(vm.index) { vm.index.notes.values.sortedBy { it.name.lowercase() } }
    val q = query.trim().lowercase()
    val shown = if (q.isEmpty()) all
    else all.mapNotNull { n -> Fuzzy.score(q, n.name.lowercase())?.let { n to it } }.sortedBy { it.second }.map { it.first }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add note to canvas") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Find a note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    Modifier
                        .heightIn(max = 360.dp)
                        .padding(top = 8.dp),
                ) {
                    items(shown, key = { it.file.absolutePath }) { n ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(n.file) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Lucide.FileText, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(n.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
