package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.PanelSettings
import com.example.model.WindowGeometry

/** Remembers panel size, position and options across app restarts. */
class SettingsStore(ctx: Context) {
    private val p: SharedPreferences = ctx.getSharedPreferences("panel_settings", Context.MODE_PRIVATE)

    private fun SharedPreferences.geo(k: String, d: WindowGeometry): WindowGeometry {
        val v = getString(k, null)?.split(',')?.mapNotNull { it.toFloatOrNull() } ?: return d
        return if (v.size == 4) WindowGeometry(v[0], v[1], v[2], v[3]) else d
    }

    private fun geoText(g: WindowGeometry) = "${g.xOffset},${g.yOffset},${g.widthDp},${g.heightDp}"

    fun load(): PanelSettings {
        val d = PanelSettings()
        return d.copy(
            roundedCorners = p.getInt("roundedCorners", d.roundedCorners),
            horizontalOffset = p.getInt("horizontalOffset", d.horizontalOffset),
            verticalPosition = p.getInt("verticalPosition", d.verticalPosition),
            columns = p.getInt("columns", d.columns),
            rows = p.getInt("rows", d.rows),
            isRightEdge = p.getBoolean("isRightEdge", d.isRightEdge),
            handleOpacity = p.getFloat("handleOpacity", d.handleOpacity),
            handleHeightDp = p.getFloat("handleHeightDp", d.handleHeightDp),
            autoDetectCategories = p.getBoolean("autoDetectCategories", d.autoDetectCategories),
            mainPanelGeometry = p.geo("mainPanelGeometry", d.mainPanelGeometry),
            mediaWindowGeometry = p.geo("mediaWindowGeometry", d.mediaWindowGeometry),
            quickAccessGeometry = p.geo("quickAccessGeometry", d.quickAccessGeometry),
            settingsGeometry = p.geo("settingsGeometry", d.settingsGeometry),
            isTopMediaSectionVisible = p.getBoolean("isTopMediaSectionVisible", d.isTopMediaSectionVisible),
            isBottomQuickSettingsVisible = p.getBoolean("isBottomQuickSettingsVisible", d.isBottomQuickSettingsVisible),
            backdropOpacityPercent = p.getInt("backdropOpacityPercent", d.backdropOpacityPercent),
            isEdgeHandleEnabled = p.getBoolean("isEdgeHandleEnabled", d.isEdgeHandleEnabled)
        )
    }

    fun save(s: PanelSettings) {
        p.edit()
            .putInt("roundedCorners", s.roundedCorners)
            .putInt("horizontalOffset", s.horizontalOffset)
            .putInt("verticalPosition", s.verticalPosition)
            .putInt("columns", s.columns)
            .putInt("rows", s.rows)
            .putBoolean("isRightEdge", s.isRightEdge)
            .putFloat("handleOpacity", s.handleOpacity)
            .putFloat("handleHeightDp", s.handleHeightDp)
            .putBoolean("autoDetectCategories", s.autoDetectCategories)
            .putString("mainPanelGeometry", geoText(s.mainPanelGeometry))
            .putString("mediaWindowGeometry", geoText(s.mediaWindowGeometry))
            .putString("quickAccessGeometry", geoText(s.quickAccessGeometry))
            .putString("settingsGeometry", geoText(s.settingsGeometry))
            .putBoolean("isTopMediaSectionVisible", s.isTopMediaSectionVisible)
            .putBoolean("isBottomQuickSettingsVisible", s.isBottomQuickSettingsVisible)
            .putInt("backdropOpacityPercent", s.backdropOpacityPercent)
            .putBoolean("isEdgeHandleEnabled", s.isEdgeHandleEnabled)
            .apply()
    }
}
