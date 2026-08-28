package com.charlztech.tv

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.charlztech.tv.player.VlcBootstrap
import com.charlztech.tv.resources.Strings
import com.charlztech.tv.ui.navigation.CharlzTechNavHost
import com.charlztech.tv.ui.theme.AppColors
import com.charlztech.tv.ui.theme.CharlzTechTvTheme
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent as AwtKeyEvent

fun main() {
    // Must run before any VLCJ / JNA native load (AppContainer creates the player).
    VlcBootstrap.setup()
    application {
        val container = remember { AppContainer(AppContainer.defaultDataDir()).also { it.start() } }
        val appWindow = remember { AppWindowState() }
        val windowState = rememberWindowState(width = 1680.dp, height = 1020.dp)

        LaunchedEffect(appWindow.isWindowFullscreen) {
            windowState.placement = if (appWindow.isWindowFullscreen) {
                WindowPlacement.Maximized
            } else {
                WindowPlacement.Floating
            }
        }

        // Swing / VLC steals keyboard focus — catch Escape/F11 at the AWT level so exit always works.
        DisposableEffect(appWindow) {
            val dispatcher = KeyEventDispatcher { event ->
                if (event.id != AwtKeyEvent.KEY_PRESSED) return@KeyEventDispatcher false
                when (event.keyCode) {
                    AwtKeyEvent.VK_ESCAPE -> {
                        if (appWindow.isVideoFullscreen) {
                            appWindow.exitVideoFullscreen()
                            true
                        } else {
                            false
                        }
                    }
                    AwtKeyEvent.VK_F11 -> {
                        if (appWindow.isVideoFullscreen) {
                            appWindow.exitVideoFullscreen()
                        } else {
                            appWindow.toggleWindowFullscreen()
                        }
                        true
                    }
                    else -> false
                }
            }
            val manager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
            manager.addKeyEventDispatcher(dispatcher)
            onDispose { manager.removeKeyEventDispatcher(dispatcher) }
        }

        Window(
            onCloseRequest = {
                container.shutdown()
                exitApplication()
            },
            title = Strings.appName,
            state = windowState,
            onPreviewKeyEvent = { event ->
                if (event.type != KeyEventType.KeyDown) return@Window false
                when (event.key) {
                    Key.Escape -> {
                        if (appWindow.isVideoFullscreen) {
                            appWindow.exitVideoFullscreen()
                            true
                        } else {
                            false
                        }
                    }
                    Key.F11 -> {
                        if (appWindow.isVideoFullscreen) {
                            appWindow.exitVideoFullscreen()
                        } else {
                            appWindow.toggleWindowFullscreen()
                        }
                        true
                    }
                    else -> false
                }
            }
        ) {
            CompositionLocalProvider(
                LocalAppContainer provides container,
                LocalAppWindowState provides appWindow
            ) {
                CharlzTechTvTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = AppColors.Background
                    ) {
                        CharlzTechNavHost()
                    }
                }
            }
        }
    }
}
