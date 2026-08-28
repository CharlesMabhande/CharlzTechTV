package com.charlztech.charlztechtv.player

import com.charlztech.charlztechtv.data.model.PlaybackRequest

/**
 * Holds the pending playback payload in memory.
 * Avoids passing [PlaybackRequest] through Intent Serializable (crashes on many devices).
 */
object PlaybackSession {
    @Volatile
    private var pending: PlaybackRequest? = null

    fun set(request: PlaybackRequest) {
        pending = request
    }

    fun consume(): PlaybackRequest? {
        val value = pending
        pending = null
        return value
    }
}
