package com.v2ray.ang.ui

import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable

class SettingsActivity : BaseComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @Composable
    override fun ScreenContent() {
        A4SettingsScreen(
            onBackClick = { finish() },
            onOpenLogcat = { startActivity(Intent(this, LogcatActivity::class.java)) },
        )
    }
}
