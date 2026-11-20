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
            isEdgeHandleEnabled = p.getBoolean("isEdgeHandleEnabled", d.isEdgeHandleEnabled),
            rightTriggerVerticalPos = p.getInt("rightTriggerVerticalPos", d.rightTriggerVerticalPos),
            rightTriggerWidthDp = p.getFloat("rightTriggerWidthDp", d.rightTriggerWidthDp),
            rightTriggerLengthDp = p.getFloat("rightTriggerLengthDp", d.rightTriggerLengthDp),
            leftTriggerVerticalPos = p.getInt("leftTriggerVerticalPos", d.leftTriggerVerticalPos),
            leftTriggerWidthDp = p.getFloat("leftTriggerWidthDp", d.leftTriggerWidthDp),
            leftTriggerLengthDp = p.getFloat("leftTriggerLengthDp", d.leftTriggerLengthDp),
            panelHeightPercent = p.getInt("panelHeightPercent", d.panelHeightPercent),
            panelWidthDp = p.getFloat("panelWidthDp", d.panelWidthDp),
            verticalOffsetDp = p.getInt("verticalOffsetDp", d.verticalOffsetDp),
            floatingLengthDp = p.getFloat("floatingLengthDp", d.floatingLengthDp),
            floatingWidthDp = p.getFloat("floatingWidthDp", d.floatingWidthDp),
            quickToolLengthDp = p.getFloat("quickToolLengthDp", d.quickToolLengthDp),
            quickToolWidthDp = p.getFloat("quickToolWidthDp", d.quickToolWidthDp),
            mediaControlLengthDp = p.getFloat("mediaControlLengthDp", d.mediaControlLengthDp),
            mediaControlWidthDp = p.getFloat("mediaControlWidthDp", d.mediaControlWidthDp),
            rememberFloatingPosition = p.getBoolean("rememberFloatingPosition", d.rememberFloatingPosition),
            openFloatingAppsBottomDown = p.getBoolean("openFloatingAppsBottomDown", d.openFloatingAppsBottomDown),
            oneHandSize = p.getFloat("oneHandSize", d.oneHandSize),
            syncWithEdgePanel = p.getBoolean("syncWithEdgePanel", d.syncWithEdgePanel),
            showApkNameInGroupList = p.getBoolean("showApkNameInGroupList", d.showApkNameInGroupList),
            showActionNameInList = p.getBoolean("showActionNameInList", d.showActionNameInList),
            showActiveMediaInQuickPanel = p.getBoolean("showActiveMediaInQuickPanel", d.showActiveMediaInQuickPanel),
            showActiveMediaInMediaControl = p.getBoolean("showActiveMediaInMediaControl", d.showActiveMediaInMediaControl)
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
            .putInt("rightTriggerVerticalPos", s.rightTriggerVerticalPos)
            .putFloat("rightTriggerWidthDp", s.rightTriggerWidthDp)
            .putFloat("rightTriggerLengthDp", s.rightTriggerLengthDp)
            .putInt("leftTriggerVerticalPos", s.leftTriggerVerticalPos)
            .putFloat("leftTriggerWidthDp", s.leftTriggerWidthDp)
            .putFloat("leftTriggerLengthDp", s.leftTriggerLengthDp)
            .putInt("panelHeightPercent", s.panelHeightPercent)
            .putFloat("panelWidthDp", s.panelWidthDp)
            .putInt("verticalOffsetDp", s.verticalOffsetDp)
            .putFloat("floatingLengthDp", s.floatingLengthDp)
            .putFloat("floatingWidthDp", s.floatingWidthDp)
            .putFloat("quickToolLengthDp", s.quickToolLengthDp)
            .putFloat("quickToolWidthDp", s.quickToolWidthDp)
            .putFloat("mediaControlLengthDp", s.mediaControlLengthDp)
            .putFloat("mediaControlWidthDp", s.mediaControlWidthDp)
            .putBoolean("rememberFloatingPosition", s.rememberFloatingPosition)
            .putBoolean("openFloatingAppsBottomDown", s.openFloatingAppsBottomDown)
            .putFloat("oneHandSize", s.oneHandSize)
            .putBoolean("syncWithEdgePanel", s.syncWithEdgePanel)
            .putBoolean("showApkNameInGroupList", s.showApkNameInGroupList)
            .putBoolean("showActionNameInList", s.showActionNameInList)
            .putBoolean("showActiveMediaInQuickPanel", s.showActiveMediaInQuickPanel)
            .putBoolean("showActiveMediaInMediaControl", s.showActiveMediaInMediaControl)
            .apply()
    }
}
