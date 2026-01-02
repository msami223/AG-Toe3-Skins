package com.devstormtech.toe3skins

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Singleton manager for AdMob interstitial ads with Yandex mediation
 */
object AdManager {
    private const val TAG = "AdManager"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-1527675586070314/1000141264"
    
    // For testing, use test ad unit ID:
    // private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var isInitialized = false
    
    // Counter to show ads periodically (e.g., every 3 actions)
    private var actionCounter = 0
    private const val ACTIONS_BETWEEN_ADS = 3

    /**
     * Initialize Mobile Ads SDK - call this in Application or MainActivity onCreate
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob initialized: ${initializationStatus.adapterStatusMap}")
            isInitialized = true
            // Pre-load the first ad
            loadInterstitialAd(context)
        }
    }

    /**
     * Load an interstitial ad
     */
    fun loadInterstitialAd(context: Context) {
        if (isLoading || interstitialAd != null) {
            Log.d(TAG, "Ad already loading or loaded")
            return
        }

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    interstitialAd = ad
                    isLoading = false
                    setupFullScreenCallback(context)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Failed to load interstitial ad: ${adError.message}")
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    private fun setupFullScreenCallback(context: Context) {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed, loading next one")
                interstitialAd = null
                // Load the next ad
                loadInterstitialAd(context)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Failed to show ad: ${adError.message}")
                interstitialAd = null
                loadInterstitialAd(context)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed successfully")
            }
        }
    }

    /**
     * Show interstitial ad immediately if loaded
     * @return true if ad was shown, false otherwise
     */
    fun showInterstitialAd(activity: Activity): Boolean {
        return if (interstitialAd != null) {
            interstitialAd?.show(activity)
            true
        } else {
            Log.d(TAG, "Interstitial ad not ready yet")
            loadInterstitialAd(activity)
            false
        }
    }

    /**
     * Increment action counter and show ad if threshold reached
     * Call this after user actions like: save, export, download, etc.
     */
    fun onUserAction(activity: Activity) {
        actionCounter++
        Log.d(TAG, "Action count: $actionCounter")
        
        if (actionCounter >= ACTIONS_BETWEEN_ADS) {
            if (showInterstitialAd(activity)) {
                actionCounter = 0 // Reset counter after showing ad
            }
        }
    }

    /**
     * Force show ad immediately (for specific events)
     */
    fun forceShowAd(activity: Activity) {
        showInterstitialAd(activity)
    }

    /**
     * Check if an ad is ready to show
     */
    fun isAdReady(): Boolean = interstitialAd != null
}
