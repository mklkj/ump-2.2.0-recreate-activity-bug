package io.github.mklkj.ump220bug

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class AdsHelper(
    private val activity: Activity,
    private val context: Context,
) {

    private var isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var consentInformation: ConsentInformation? = null

    private val canRequestAd get() = consentInformation?.canRequestAds() == true
    val isMobileAdsSdkInitialized = MutableStateFlow(false)
    val canShowAd get() = isMobileAdsSdkInitialized.value && canRequestAd

    init {
//        if (preferencesRepository.isAdsEnabled) {
        initialize()
//        }
    }

    fun initialize() {
        val consentRequestParameters = ConsentRequestParameters.Builder()
            .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(context)
        consentInformation?.requestConsentInfoUpdate(
            activity,
            consentRequestParameters,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    activity
                ) { loadAndShowError ->

                    if (loadAndShowError != null) {
                        Log.e(
                            "AdsHelper",
                            loadAndShowError.message,
                            IllegalStateException("${loadAndShowError.errorCode}: ${loadAndShowError.message}")
                        )
                    }

                    if (canRequestAd) {
                        initializeMobileAds()
                    }
                }
            },
            { requestConsentError ->
                Log.e(
                    "AdsHelper",
                    requestConsentError.message,
                    IllegalStateException("${requestConsentError.errorCode}: ${requestConsentError.message}")
                )
            })

        if (canRequestAd) {
            initializeMobileAds()
        }
    }

    fun openAdsUmpAgreements() {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            if (it != null) {
                Log.e(
                    "AdsHelper",
                    it.message,
                    IllegalStateException("${it.errorCode}: ${it.message}")
                )
            }
        }
    }

    private fun initializeMobileAds() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) return

        MobileAds.initialize(context) {
            isMobileAdsSdkInitialized.value = true
        }
    }

    suspend fun getDashboardTileAdBanner(width: Int): AdBanner {
        if (!canShowAd) throw IllegalStateException("Cannot show ad")
        val adRequest = AdRequest.Builder()
            .build()

        return suspendCoroutine {
            val adView = AdView(context).apply {
                setAdSize(AdSize.getPortraitAnchoredAdaptiveBannerAdSize(context, width))
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        it.resumeWithException(IllegalArgumentException(loadAdError.message))
                    }

                    override fun onAdLoaded() {
                        it.resume(AdBanner(this@apply))
                    }
                }
            }

            adView.loadAd(adRequest)
        }
    }
}

data class AdBanner(val view: View)
