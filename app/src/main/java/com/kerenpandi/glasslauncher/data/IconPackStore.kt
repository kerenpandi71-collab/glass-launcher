package com.kerenpandi.glasslauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import java.util.concurrent.ConcurrentHashMap

object IconPackStore {

    private const val ACTION_ADW_THEME = "org.adw.launcher.THEMES"

    private val cache = ConcurrentHashMap<String, Map<String, Drawable>>()

    fun installedPacks(context: Context): List<Pair<String, String>> {
        val pm = context.packageManager
        return runCatching {
            pm.queryIntentActivities(Intent(ACTION_ADW_THEME), 0)
                .mapNotNull { info ->
                    val pkg = info.activityInfo.packageName
                    if (pkg == context.packageName) null else pkg to info.loadLabel(pm).toString()
                }
                .distinctBy { it.first }
                .sortedBy { it.second.lowercase() }
        }.getOrDefault(emptyList())
    }

    fun iconFor(context: Context, packPackage: String, app: AppInfo): Drawable? {
        if (packPackage.isBlank()) return null
        val mapping = runCatching { cache.getOrPut(packPackage) { loadPack(context, packPackage) } }
            .getOrNull() ?: return null
        return mapping[app.componentName.flattenToString()]
    }

    private fun loadPack(context: Context, packPackage: String): Map<String, Drawable> {
        val pm = context.packageManager
        val resources = pm.getResourcesForApplication(packPackage)
        val xmlId = resources.getIdentifier("appfilter", "xml", packPackage)
        if (xmlId == 0) return emptyMap()
        val result = mutableMapOf<String, Drawable>()
        val parser: XmlResourceParser = resources.getXml(xmlId)
        try {
            var event = parser.eventType
            while (event != XmlResourceParser.END_DOCUMENT) {
                if (event == XmlResourceParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component").orEmpty()
                    val drawable = parser.getAttributeValue(null, "drawable").orEmpty()
                    if (component.isNotBlank() && drawable.isNotBlank()) {
                        val raw = component.removePrefix("ComponentInfo{").removeSuffix("}")
                        val keys = buildSet {
                            add(raw)
                            val parts = raw.split("/", limit = 2)
                            if (parts.size == 2) {
                                val (pkg, cls) = parts
                                if (cls.startsWith(".")) add("$pkg/$pkg$cls")
                            }
                        }
                        val id = resources.getIdentifier(drawable, "drawable", packPackage)
                        if (id != 0) {
                            runCatching { resources.getDrawable(id) }.getOrNull()?.let { icon ->
                                keys.forEach { key -> result[key] = icon }
                            }
                        }
                    }
                }
                event = parser.next()
            }
        } finally {
            parser.close()
        }
        return result
    }
}
