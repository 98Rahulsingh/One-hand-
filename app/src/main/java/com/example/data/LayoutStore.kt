package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Remembers groups, which apps are in which group, and which quick tiles are shown. */
class LayoutStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("layout", Context.MODE_PRIVATE)

    fun saveList(k: String, v: List<String>) { p.edit().putString(k, JSONArray(v).toString()).apply() }

    fun loadList(k: String): List<String>? = p.getString(k, null)?.let { s ->
        val a = JSONArray(s)
        List(a.length()) { a.getString(it) }
    }

    fun saveSet(k: String, v: Set<String>) = saveList(k, v.toList())
    fun loadSet(k: String): Set<String>? = loadList(k)?.toSet()

    fun saveMap(k: String, v: Map<String, Set<String>>) {
        val o = JSONObject()
        v.forEach { (g, s) -> o.put(g, JSONArray(s.toList())) }
        p.edit().putString(k, o.toString()).apply()
    }

    fun loadMap(k: String): Map<String, Set<String>> {
        val s = p.getString(k, null) ?: return emptyMap()
        val o = JSONObject(s)
        val out = mutableMapOf<String, Set<String>>()
        o.keys().forEach { g ->
            val a = o.getJSONArray(g)
            out[g] = List(a.length()) { a.getString(it) }.toSet()
        }
        return out
    }
}
