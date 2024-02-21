package io.github.mklkj.ump220bug

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var adsHelper: AdsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adsHelper = AdsHelper(this, this)

        if (!adsHelper.canShowAd) {
            adsHelper.openAdsUmpAgreements()
        }

        lifecycleScope.launch {
            adsHelper.isMobileAdsSdkInitialized.collectLatest {
                if (it) {
                    addAd()
                }
            }
        }
    }

    private fun addAd() {
        val adContainer = findViewById<FrameLayout>(R.id.container)
        lifecycleScope.launch {
            val banner = adsHelper.getDashboardTileAdBanner(350)
            adContainer.removeAllViews()
            adContainer.addView(banner.view)
        }
    }
}
