package dev.lurexh.halo.core

object Native {
    init {
        System.loadLibrary("halo_core")
    }

    external fun startTunnel(fd: Int, configJson: String): Int

    external fun stopTunnel()

    external fun version(): String
}
