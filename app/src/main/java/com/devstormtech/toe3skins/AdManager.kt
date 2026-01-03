package com.devstormtech.toe3skins

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.devstormtech.toe3skins.BuildConfig
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Singleton manager for AdMob interstitial ads with Yandex mediation
 */
object AdManager {
    private const val TAG = "AdManager"
    private const val PROD_INTERSTITIAL_ID = "ca-app-pub-1527675586070314/1000141264"
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    
    private const val PROD_REWARDED_ID = "ca-app-pub-1527675586070314/1167484618"
    private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    // Automatically switch based on Build Type
    private val INTERSTITIAL_AD_UNIT_ID = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else PROD_INTERSTITIAL_ID
    private val REWARDED_AD_UNIT_ID = if (BuildConfig.DEBUG) TEST_REWARDED_ID else PROD_REWARDED_ID
    
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    
    private var isLoadingInterstitial = false
    private var isLoadingRewarded = false
    private var isInitialized = false
    
    // Counter to show ads periodically (More Frequent: every 2 actions)
    private var actionCounter = 0
    private const val ACTIONS_BETWEEN_ADS = 4

    /**
     * Initialize Mobile Ads SDK - call this in Application or MainActivity onCreate
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        MobileAds.initialize(context) { initializationStatus ->
            Log.d(TAG, "AdMob initialized: ${initializationStatus.adapterStatusMap}")
            isInitialized = true
            // Pre-load ads
            loadInterstitialAd(context)
            loadRewardedAd(context)
        }
    }

    /**
     * Load an interstitial ad
     */
    fun loadInterstitialAd(context: Context) {
        if (isLoadingInterstitial || interstitialAd != null) {
            return
        }

        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial ad loaded successfully")
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    setupInterstitialCallback(context)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Failed to load interstitial ad: ${adError.message}")
                    interstitialAd = null
                    isLoadingInterstitial = false
                }
            }
        )
    }

    /**
     * Load a rewarded ad
     */
    fun loadRewardedAd(context: Context) {
        if (isLoadingRewarded || rewardedAd != null) {
            return
        }

        isLoadingRewarded = true
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Failed to load rewarded ad: ${adError.message}")
                    rewardedAd = null
                    isLoadingRewarded = false
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad loaded successfully")
                    rewardedAd = ad
                    isLoadingRewarded = false
                    setupRewardedCallback(context)
                }
            }
        )
    }

    private fun setupInterstitialCallback(context: Context) {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial Dismissed")
                interstitialAd = null
                loadInterstitialAd(context)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                interstitialAd = null
                loadInterstitialAd(context)
            }
        }
    }

    private fun setupRewardedCallback(context: Context) {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded Ad Dismissed")
                rewardedAd = null
                loadRewardedAd(context)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadRewardedAd(context)
            }
        }
    }

    /**
     * Show interstitial ad
     */
    fun showInterstitialAd(activity: Activity): Boolean {
        return if (interstitialAd != null) {
            interstitialAd?.show(activity)
            true
        } else {
            Log.d(TAG, "Interstitial not ready")
            loadInterstitialAd(activity)
            false
        }
    }

    /**
     * Show Rewarded Ad.
     * FAIL-SAFE: If ad is not ready, we immediately grant the reward (FAIL OPEN).
     */
    fun showRewardedAd(activity: Activity, onUserEarnedReward: (Boolean) -> Unit) {
        if (rewardedAd != null) {
            rewardedAd?.show(activity) { rewardItem ->
                // Handle the reward.
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                // Reset interstitial counter to prevent double-ad annoyance
                actionCounter = 0
                onUserEarnedReward(true)
            }
        } else {
            Log.d(TAG, "Rewarded ad not ready - FAILING OPEN (Granting reward anyway)")
            loadRewardedAd(activity)
            // FAIL OPEN: Grant reward immediately so user is not blocked
            onUserEarnedReward(true)
        }
    }

    /**
     * Increment action counter and show interstitial ad if threshold reached.
     * Interstitials will NOT show if we just showed a Rewarded Ad (counter reset).
     */
    fun onUserAction(activity: Activity) {
        actionCounter++
        
        if (actionCounter >= ACTIONS_BETWEEN_ADS) {
            if (showInterstitialAd(activity)) {
                actionCounter = 0 
            }
        }
    }
}
