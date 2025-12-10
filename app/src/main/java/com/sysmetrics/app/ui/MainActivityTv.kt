package com.sysmetrics.app.ui

import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.sysmetrics.app.R
import com.sysmetrics.app.ui.home.HomeTvFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for Android TV
 * Hosts the TV-optimized home fragment with D-pad navigation
 */
@AndroidEntryPoint
class MainActivityTv : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_tv)

        // Load home fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeTvFragment())
                .commit()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Let fragments handle D-pad navigation first
        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (fragment != null && fragment.isVisible) {
            // Fragment will handle via DpadNavigationHandler
            return super.onKeyDown(keyCode, event)
        }
        
        return super.onKeyDown(keyCode, event)
    }
}
