package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.PanelSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsStoreFeatureSwitchTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private fun clearSettings() {
        context.getSharedPreferences("panel_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun featureSwitchesDefaultToOnForExistingUsers() {
        clearSettings()

        val settings = SettingsStore(context).load()

        assertTrue(settings.isAppLauncherEnabled)
        assertTrue(settings.isAlphabetScrollerEnabled)
        assertTrue(settings.isCustomGroupsEnabled)
        assertTrue(settings.isActiveMediaEnabled)
        assertTrue(settings.isTopMediaSectionVisible)
        assertTrue(settings.isBottomQuickSettingsVisible)
        assertTrue(settings.isEdgeHandleEnabled)
    }

    @Test
    fun featureSwitchesPersistOnThisDevice() {
        clearSettings()
        val expected = PanelSettings().copy(
            isAppLauncherEnabled = false,
            isAlphabetScrollerEnabled = false,
            isCustomGroupsEnabled = false,
            isActiveMediaEnabled = false,
            isTopMediaSectionVisible = false,
            isBottomQuickSettingsVisible = false,
            isEdgeHandleEnabled = false
        )

        SettingsStore(context).save(expected)

        assertEquals(expected, SettingsStore(context).load())
    }
}
