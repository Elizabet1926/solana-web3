package com.elizabet1926.solana

import android.app.Application

class SolanaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RPCConfigManager.init(this)
    }
}
