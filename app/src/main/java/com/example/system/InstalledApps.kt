package com.example.system

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.model.AppItem
import com.example.model.ItemType
import java.util.concurrent.ConcurrentHashMap

/** Real app icons, loaded once and shown by the panel. */
object AppIcons {
    private val map = ConcurrentHashMap<String, ImageBitmap>()
    fun get(pkg: String): ImageBitmap? = if (pkg.isBlank()) null else map[pkg]
    fun put(pkg: String, bmp: ImageBitmap) { map[pkg] = bmp }
}

/** Reads the apps really installed on the phone, with real usage numbers. */
class InstalledApps(private val ctx: Context) {

    /** package -> (minutes used in last 7 days, last used time). Empty if Usage access is off. */
    fun usageMap(): Map<String, Pair<Long, Long>> = try {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, end - 7 * 86_400_000L, end).orEmpty()
            .groupBy { it.packageName }
            .mapValues { e -> Pair(e.value.sumOf { it.totalTimeInForeground } / 60_000L, e.value.maxOf { it.lastTimeUsed }) }
    } catch (_: Exception) {
        emptyMap()
    }

    /** package -> last time it was opened (looks back one year). 0 means never opened. */
    fun lastUsedMap(): Map<String, Long> = try {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        usm.queryAndAggregateUsageStats(end - 365 * 86_400_000L, end).mapValues { it.value.lastTimeUsed }
    } catch (_: Exception) {
        emptyMap()
    }

    private fun groupFor(category: Int): String? = when (category) {
        ApplicationInfo.CATEGORY_GAME -> "Games"
        ApplicationInfo.CATEGORY_AUDIO -> "Music"
        ApplicationInfo.CATEGORY_VIDEO -> "Video"
        ApplicationInfo.CATEGORY_IMAGE -> "Images"
        ApplicationInfo.CATEGORY_SOCIAL, ApplicationInfo.CATEGORY_NEWS -> "Social"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
        ApplicationInfo.CATEGORY_MAPS, ApplicationInfo.CATEGORY_ACCESSIBILITY -> "Tools"
        else -> null
    }

    fun load(): List<AppItem> {
        val pm = ctx.packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val usage = usageMap()
        val last = lastUsedMap()
        return pm.queryIntentActivities(q, 0)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { ri ->
                val pkg = ri.activityInfo.packageName
                try { AppIcons.put(pkg, ri.loadIcon(pm).toBitmap(96, 96).asImageBitmap()) } catch (_: Exception) {}
                val group = groupFor(ri.activityInfo.applicationInfo.category)
                AppItem(
                    id = "app_$pkg",
                    name = ri.loadLabel(pm).toString(),
                    packageName = pkg,
                    type = ItemType.APP,
                    categoryName = group ?: "Tools",
                    iconName = "apps",
                    iconColor = 0xFF00BCD4,
                    isAdded = group != null,
                    subtitle = "",
                    usageCount = (usage[pkg]?.first ?: 0L).toInt(),
                    lastUsed = last[pkg] ?: 0L
                )
            }
            .sortedBy { it.name.lowercase() }
    }
}
