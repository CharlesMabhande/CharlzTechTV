package com.charlztech.tv.player

import java.nio.file.Files
import java.nio.file.Path

/**
 * Points VLCJ/JNA at the VLC natives shipped inside the Windows installer
 * (`compose.application.resources.dir`), so playback works without a
 * separate system VLC install. Falls back to NativeDiscovery when running
 * from the IDE / system VLC.
 */
object VlcBootstrap {
    fun setup() {
        val resourcesDir = System.getProperty("compose.application.resources.dir")
            ?.takeIf { it.isNotBlank() }
            ?.let { Path.of(it) }
            ?: return

        val libVlc = resourcesDir.resolve("libvlc.dll")
        if (!Files.isRegularFile(libVlc)) return

        System.setProperty("jna.library.path", resourcesDir.toAbsolutePath().toString())

        val plugins = resourcesDir.resolve("plugins")
        if (Files.isDirectory(plugins)) {
            val pluginPath = plugins.toAbsolutePath().toString()
            System.setProperty("VLC_PLUGIN_PATH", pluginPath)
            // Some VLC builds also honor this env var for plugin discovery.
            runCatching {
                val processEnv = ProcessBuilder().environment()
                processEnv["VLC_PLUGIN_PATH"] = pluginPath
            }
        }
    }

    fun pluginPathArg(): String? =
        System.getProperty("VLC_PLUGIN_PATH")?.takeIf { it.isNotBlank() }?.let { "--plugin-path=$it" }
}
