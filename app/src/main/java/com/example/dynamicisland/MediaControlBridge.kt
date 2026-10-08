package com.example.dynamicisland

object MediaControlBridge {
    private var service: MediaListenerService? = null

    fun attach(svc: MediaListenerService) { service = svc }
    fun detach() { service = null }

    fun playPause() { service?.playPause() }
    fun skipNext() { service?.skipNext() }
    fun skipPrevious() { service?.skipPrevious() }

    val isReady: Boolean
        get() = service != null
}
