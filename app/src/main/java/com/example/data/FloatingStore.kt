package com.example.data

import android.content.Context
import android.graphics.Rect

/** Remembers where each app's floating window was last (position and size). */
class FloatingStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("floating_bounds", Context.MODE_PRIVATE)

    fun get(pkg: String): Rect? {
        val v = p.getString(pkg, null)?.split(',')?.mapNotNull { it.toIntOrNull() } ?: return null
        return if (v.size == 4) Rect(v[0], v[1], v[2], v[3]) else null
    }

    fun put(pkg: String, r: Rect) {
        p.edit().putString(pkg, "${r.left},${r.top},${r.right},${r.bottom}").apply()
    }
}
