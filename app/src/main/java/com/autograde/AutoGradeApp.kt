package com.autograde

import android.app.Application
import com.autograde.data.local.TokenStore
import com.autograde.network.RetrofitProvider

class AutoGradeApp : Application() {
    lateinit var tokenStore: TokenStore
    val api by lazy { RetrofitProvider.create(tokenStore) }

    override fun onCreate() {
        super.onCreate()
        tokenStore = TokenStore(this)
    }
}
