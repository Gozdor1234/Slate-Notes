package com.gozdor.slatenotes.data

import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt
import kotlin.random.Random

/** A card on a canvas. Positions and sizes are in canvas units (JSON Canvas 1.0 "pixels"). */
data class CNode(
    val id: String,
    val type: String,
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val color: String?,
    val text: String,
    val file: String,
    val url: String,
    val label: String,
    /** The node exactly as read, so fields Slate doesn't know about survive a save. */
    val raw: JSONObject,
)

data class CEdge(
    val id: String,
    val from: String,
    val to: String,
    val fromSide: String?,
    val toSide: String?,
    val fromEnd: String?,
    val toEnd: String?,
    val color: String?,
    val label: String?,
    val raw: JSONObject,
)

class CanvasDoc(val root: JSONObject, val nodes: List<CNode>, val edges: List<CEdge>)

/** Reads and writes .canvas files (JSON Canvas 1.0, the format Obsidian uses). */
object CanvasFile {
    const val EMPTY = "{\"nodes\":[],\"edges\":[]}"

    private fun JSONObject.str(k: String): String? = if (has(k) && !isNull(k)) optString(k) else null

    fun parse(text: String): CanvasDoc {
        val root = if (text.isBlank()) JSONObject() else JSONObject(text)
        val na = root.optJSONArray("nodes") ?: JSONArray()
        val nodes = (0 until na.length()).mapNotNull { i ->
            val o = na.optJSONObject(i) ?: return@mapNotNull null
            CNode(
                id = o.str("id") ?: newId(),
                type = o.str("type") ?: "text",
                x = o.optDouble("x", 0.0).toFloat(),
                y = o.optDouble("y", 0.0).toFloat(),
                w = o.optDouble("width", 250.0).toFloat(),
                h = o.optDouble("height", 60.0).toFloat(),
                color = o.str("color"),
                text = o.str("text") ?: "",
                file = o.str("file") ?: "",
                url = o.str("url") ?: "",
                label = o.str("label") ?: "",
                raw = o,
            )
        }
        val ea = root.optJSONArray("edges") ?: JSONArray()
        val edges = (0 until ea.length()).mapNotNull { i ->
            val o = ea.optJSONObject(i) ?: return@mapNotNull null
            val from = o.str("fromNode") ?: return@mapNotNull null
            val to = o.str("toNode") ?: return@mapNotNull null
            CEdge(
                id = o.str("id") ?: newId(),
                from = from,
                to = to,
                fromSide = o.str("fromSide"),
                toSide = o.str("toSide"),
                fromEnd = o.str("fromEnd"),
                toEnd = o.str("toEnd"),
                color = o.str("color"),
                label = o.str("label"),
                raw = o,
            )
        }
        return CanvasDoc(root, nodes, edges)
    }

    fun serialize(root: JSONObject, nodes: List<CNode>, edges: List<CEdge>): String {
        val out = JSONObject(root.toString())
        val na = JSONArray()
        for (n in nodes) {
            val o = JSONObject(n.raw.toString())
            o.put("id", n.id)
            o.put("type", n.type)
            o.put("x", n.x.roundToInt())
            o.put("y", n.y.roundToInt())
            o.put("width", n.w.roundToInt())
            o.put("height", n.h.roundToInt())
            putOrRemove(o, "color", n.color)
            when (n.type) {
                "text" -> o.put("text", n.text)
                "file" -> o.put("file", n.file)
                "link" -> o.put("url", n.url)
                "group" -> putOrRemove(o, "label", n.label.ifEmpty { null })
            }
            na.put(o)
        }
        val ea = JSONArray()
        for (e in edges) {
            val o = JSONObject(e.raw.toString())
            o.put("id", e.id)
            o.put("fromNode", e.from)
            o.put("toNode", e.to)
            putOrRemove(o, "fromSide", e.fromSide)
            putOrRemove(o, "toSide", e.toSide)
            putOrRemove(o, "fromEnd", e.fromEnd)
            putOrRemove(o, "toEnd", e.toEnd)
            putOrRemove(o, "color", e.color)
            putOrRemove(o, "label", e.label)
            ea.put(o)
        }
        out.put("nodes", na)
        out.put("edges", ea)
        return out.toString(2)
    }

    private fun putOrRemove(o: JSONObject, k: String, v: String?) {
        if (v == null) o.remove(k) else o.put(k, v)
    }

    /** 16 hex characters, the same shape as the ids Obsidian writes. */
    fun newId(): String = buildString { repeat(16) { append("0123456789abcdef"[Random.nextInt(16)]) } }
}
