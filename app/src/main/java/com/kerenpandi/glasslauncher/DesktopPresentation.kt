package com.kerenpandi.glasslauncher

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.kerenpandi.glasslauncher.data.AppInfo
import com.kerenpandi.glasslauncher.ui.desktop.DesktopScreen
import com.kerenpandi.glasslauncher.ui.theme.GlassTheme

class DesktopPresentation(
    outerContext: Context,
    display: Display,
    private val apps: List<AppInfo>,
    private val onLaunchApp: (AppInfo) -> Unit,
    private val onDismissed: () -> Unit = {}
) : Presentation(outerContext, display), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate(savedInstanceState: Bundle?) {
        savedStateRegistryController.performRestore(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        super.onCreate(savedInstanceState)
        val composeView = ComposeView(context)
        composeView.setViewTreeLifecycleOwner(this)
        composeView.setViewTreeSavedStateRegistryOwner(this)
        setContentView(composeView)
        composeView.setContent {
            GlassTheme {
                DesktopScreen(
                    apps = apps,
                    displayId = display.displayId,
                    onAppClick = onLaunchApp
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onResume() {
        super.onResume()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        super.onPause()
    }

    override fun onStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onStop()
    }

    override fun onDisplayRemoved() {
        super.onDisplayRemoved()
        onDismissed()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }
}
