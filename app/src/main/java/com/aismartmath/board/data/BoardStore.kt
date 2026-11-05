package com.aismartmath.board.data

import android.content.Context
import org.json.JSONObject
import java.io.File

/** The short description of a saved board (read without opening the whole board). */
class BoardMeta(val id: String, val name: String, val topicId: String, val updated: Long, val count: Int)

/**
 * Boards are saved as JSON files in the app's own storage:
 * `<id>.json` holds the drawing, `<id>.meta` holds the name and date so the list opens fast.
 */
class BoardStore(ctx: Context) {
    private val dir: File = File(ctx.filesDir, "boards").also { it.mkdirs() }

    private fun dataFile(id: String) = File(dir, "$id.json")
    private fun metaFile(id: String) = File(dir, "$id.meta")

    fun newId(): String = "b" + System.currentTimeMillis()

    private fun readMeta(f: File): BoardMeta? = try {
        val o = JSONObject(f.readText())
        BoardMeta(o.getString("id"), o.getString("name"), o.getString("topic"), o.getLong("updated"), o.optInt("count", 0))
    } catch (e: Exception) {
        null
    }

    private fun writeMeta(id: String, name: String, topicId: String, count: Int) {
        val o = JSONObject()
        o.put("id", id)
        o.put("name", name)
        o.put("topic", topicId)
        o.put("updated", System.currentTimeMillis())
        o.put("count", count)
        metaFile(id).writeText(o.toString())
    }

    fun list(): List<BoardMeta> {
        val files = dir.listFiles()?.filter { it.name.endsWith(".meta") } ?: emptyList()
        return files.mapNotNull { readMeta(it) }.sortedByDescending { it.updated }
    }

    fun meta(id: String): BoardMeta? {
        val f = metaFile(id)
        return if (f.exists()) readMeta(f) else null
    }

    fun save(id: String, name: String, topicId: String, count: Int, data: String) {
        dataFile(id).writeText(data)
        writeMeta(id, name, topicId, count)
    }

    fun load(id: String): JSONObject? = try {
        val f = dataFile(id)
        if (f.exists()) JSONObject(f.readText()) else null
    } catch (e: Exception) {
        null
    }

    fun rename(id: String, name: String) {
        val m = meta(id) ?: return
        writeMeta(id, name, m.topicId, m.count)
    }

    fun delete(id: String) {
        dataFile(id).delete()
        metaFile(id).delete()
    }

    /** Copies a board; returns the new board's id. */
    fun duplicate(id: String): String? {
        val m = meta(id) ?: return null
        val src = dataFile(id)
        if (!src.exists()) return null
        val nid = newId()
        src.copyTo(dataFile(nid), overwrite = true)
        writeMeta(nid, m.name + " copy", m.topicId, m.count)
        return nid
    }
}
