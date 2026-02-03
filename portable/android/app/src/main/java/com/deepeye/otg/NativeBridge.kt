package com.deepeye.otg

object NativeBridge {
    init {
        System.loadLibrary("deepeye_core")
    }

    external fun initCore(fd: Int, vid: Int, pid: Int): Long
    external fun identifyDevice(handle: Long): Boolean
    external fun injectDa(handle: Long, daData: ByteArray): Boolean
    external fun getPartitions(handle: Long): Array<String>
    external fun closeCore(handle: Long)
}
