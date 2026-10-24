package com.gozdor.slatenotes.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasFileTest {
    private val sample = """
        {
          "nodes": [
            {"id":"a1","type":"text","x":-10,"y":20,"width":250,"height":60,"text":"Hello **world**","color":"4","extra":"keep me"},
            {"id":"g1","type":"group","x":-100,"y":-100,"width":600,"height":400,"label":"Ideas","background":"bg.png"},
            {"id":"f1","type":"file","x":300,"y":0,"width":400,"height":300,"file":"Folder/Note.md","subpath":"#Heading"},
            {"id":"l1","type":"link","x":0,"y":400,"width":300,"height":80,"url":"https://example.com"}
          ],
          "edges": [
            {"id":"e1","fromNode":"a1","toNode":"f1","label":"see"},
            {"id":"e2","fromNode":"f1","fromSide":"bottom","toNode":"l1","toSide":"top","toEnd":"none","color":"#FF0000"}
          ],
          "futureTopLevel": {"x": 1}
        }
    """.trimIndent()

    @Test
    fun parsesAllNodeTypesAndEdges() {
        val doc = CanvasFile.parse(sample)
        assertEquals(listOf("text", "group", "file", "link"), doc.nodes.map { it.type })
        assertEquals("Hello **world**", doc.nodes[0].text)
        assertEquals("4", doc.nodes[0].color)
        assertEquals("Ideas", doc.nodes[1].label)
        assertEquals("Folder/Note.md", doc.nodes[2].file)
        assertEquals("https://example.com", doc.nodes[3].url)
        assertEquals(2, doc.edges.size)
        assertEquals(null, doc.edges[0].fromSide)
        assertEquals("none", doc.edges[1].toEnd)
    }

    @Test
    fun roundTripKeepsUnknownFieldsAndAppliesEdits() {
        val doc = CanvasFile.parse(sample)
        val moved = doc.nodes.map { if (it.id == "a1") it.copy(x = 55.4f, text = "Changed") else it }
        val out = JSONObject(CanvasFile.serialize(doc.root, moved, doc.edges))

        assertTrue(out.has("futureTopLevel"))
        val a1 = out.getJSONArray("nodes").getJSONObject(0)
        assertEquals(55, a1.getInt("x"))
        assertEquals("Changed", a1.getString("text"))
        assertEquals("keep me", a1.getString("extra"))
        val g1 = out.getJSONArray("nodes").getJSONObject(1)
        assertEquals("bg.png", g1.getString("background"))
        val f1 = out.getJSONArray("nodes").getJSONObject(2)
        assertEquals("#Heading", f1.getString("subpath"))
        assertEquals("Folder/Note.md", f1.getString("file"))

        val e1 = out.getJSONArray("edges").getJSONObject(0)
        assertFalse("sides must stay unset when they were unset", e1.has("fromSide"))
        assertFalse("toEnd defaults to arrow when absent, so don't add it", e1.has("toEnd"))
        assertEquals("see", e1.getString("label"))
    }

    @Test
    fun removedColorIsDropped() {
        val doc = CanvasFile.parse(sample)
        val nodes = doc.nodes.map { if (it.id == "a1") it.copy(color = null) else it }
        val out = JSONObject(CanvasFile.serialize(doc.root, nodes, doc.edges))
        assertFalse(out.getJSONArray("nodes").getJSONObject(0).has("color"))
    }

    @Test
    fun emptyAndMinimalFiles() {
        assertEquals(0, CanvasFile.parse("").nodes.size)
        assertEquals(0, CanvasFile.parse(CanvasFile.EMPTY).edges.size)
        val n = CanvasFile.parse("""{"nodes":[{"id":"x","type":"text","text":"hi"}]}""").nodes.single()
        assertEquals(0f, n.x)
    }

    @Test
    fun edgesMissingEndpointsAreSkipped() {
        val doc = CanvasFile.parse("""{"nodes":[],"edges":[{"id":"e","fromNode":"a"}]}""")
        assertEquals(0, doc.edges.size)
    }

    @Test
    fun idsLookLikeObsidians() {
        val id = CanvasFile.newId()
        assertEquals(16, id.length)
        assertTrue(id.all { it in "0123456789abcdef" })
    }
}
