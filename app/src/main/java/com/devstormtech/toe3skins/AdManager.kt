package com.devstormtech.toe3skins

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
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
 * Singleton manager for AdMob interstitial and rewarded ads with Yandex mediation
 * 
 * FIXED: Improved initialization, retry logic, and debugging
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
    
    // Retry configuration
    private var interstitialRetryAttempt = 0
    private var rewardedRetryAttempt = 0
    private const val MAX_RETRY_ATTEMPTS = 3
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Removed: No time limits or action counters - show ad on every user action
    
    // Context reference for reloading
    private var appContext: Context? = null

    /**
     * Check if interstitial ad is ready to be shown
     */
    val isInterstitialReady: Boolean
        get() = interstitialAd != null

    /**
     * Check if rewarded ad is ready to be shown
     */
    val isRewardedReady: Boolean
        get() = rewardedAd != null

    /**
     * Initialize Mobile Ads SDK - call this in Application or MainActivity onCreate
     * AFTER consent has been gathered
     */
    fun initialize(context: Context) {
        // Skip ad initialization entirely for internal builds (no ads)
        if (!BuildConfig.ADS_ENABLED) {
            Log.d(TAG, "Ads disabled for this build variant, skipping initialization")
            return
        }
        
        if (isInitialized) {
            Log.d(TAG, "AdManager already initialized, skipping")
            return
        }
        
        appContext = context.applicationContext
        Log.d(TAG, "Initializing AdMob SDK...")
        
        MobileAds.initialize(context) { initializationStatus ->
            isInitialized = true
            Log.d(TAG, "AdMob SDK initialized successfully")
            
            // Log adapter statuses for debugging
            initializationStatus.adapterStatusMap.forEach { (adapter, status) ->
                Log.d(TAG, "Adapter: $adapter, State: ${status.initializationState}, Latency: ${status.latency}ms")
            }
            
            // Pre-load ads after initialization
            loadInterstitialAd()
            loadRewardedAd()
        }
    }

    /**
     * Load an interstitial ad with retry logic
     */
    fun loadInterstitialAd(context: Context? = null) {
        val ctx = context ?: appContext
        if (ctx == null) {
            Log.e(TAG, "Cannot load interstitial: no context available")
            return
        }
        
        if (!isInitialized) {
            Log.w(TAG, "Cannot load interstitial: SDK not initialized yet")
            return
        }
        
        if (isLoadingInterstitial) {
            Log.d(TAG, "Interstitial already loading, skipping duplicate request")
            return
        }
        
        if (interstitialAd != null) {
            Log.d(TAG, "Interstitial already loaded and ready")
            return
        }

        isLoadingInterstitial = true
        Log.d(TAG, "Loading interstitial ad (attempt ${interstitialRetryAttempt + 1})...")
        
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            ctx,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "✓ Interstitial ad loaded successfully!")
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    interstitialRetryAttempt = 0 // Reset retry counter on success
                    setupInterstitialCallback()
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "✗ Failed to load interstitial: ${adError.code} - ${adError.message}")
                    Log.e(TAG, "  Domain: ${adError.domain}, Cause: ${adError.cause}")
                    interstitialAd = null
                    isLoadingInterstitial = false
                    
                    // Retry with exponential backoff
                    if (interstitialRetryAttempt < MAX_RETRY_ATTEMPTS) {
                        interstitialRetryAttempt++
                        val delayMs = (1000L * (1 shl interstitialRetryAttempt)) // 2s, 4s, 8s
                        Log.d(TAG, "Retrying interstitial load in ${delayMs}ms...")
                        mainHandler.postDelayed({
                            loadInterstitialAd()
                        }, delayMs)
                    } else {
                        Log.e(TAG, "Max retry attempts reached for interstitial")
                        interstitialRetryAttempt = 0 // Reset for future attempts
                    }
                }
            }
        )
    }

    /**
     * Load a rewarded ad with retry logic
     */
    fun loadRewardedAd(context: Context? = null) {
        val ctx = context ?: appContext
        if (ctx == null) {
            Log.e(TAG, "Cannot load rewarded: no context available")
            return
        }
        
        if (!isInitialized) {
            Log.w(TAG, "Cannot load rewarded: SDK not initialized yet")
            return
        }
        
        if (isLoadingRewarded) {
            Log.d(TAG, "Rewarded already loading, skipping duplicate request")
            return
        }
        
        if (rewardedAd != null) {
            Log.d(TAG, "Rewarded already loaded and ready")
            return
        }

        isLoadingRewarded = true
        Log.d(TAG, "Loading rewarded ad (attempt ${rewardedRetryAttempt + 1})...")
        
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            ctx,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "✗ Failed to load rewarded: ${adError.code} - ${adError.message}")
                    rewardedAd = null
                    isLoadingRewarded = false
                    
                    // Retry with exponential backoff
                    if (rewardedRetryAttempt < MAX_RETRY_ATTEMPTS) {
                        rewardedRetryAttempt++
                        val delayMs = (1000L * (1 shl rewardedRetryAttempt))
                        Log.d(TAG, "Retrying rewarded load in ${delayMs}ms...")
                        mainHandler.postDelayed({
                            loadRewardedAd()
                        }, delayMs)
                    } else {
                        Log.e(TAG, "Max retry attempts reached for rewarded")
                        rewardedRetryAttempt = 0
                    }
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "✓ Rewarded ad loaded successfully!")
                    rewardedAd = ad
                    isLoadingRewarded = false
                    rewardedRetryAttempt = 0
                    setupRewardedCallback()
                }
            }
        )
    }

    private fun setupInterstitialCallback() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial dismissed, preloading next one")
                interstitialAd = null
                // Immediately preload next ad
                loadInterstitialAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Interstitial failed to show: ${adError.message}")
                interstitialAd = null
                loadInterstitialAd()
            }
            
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial shown successfully")
            }
            
            override fun onAdImpression() {
                Log.d(TAG, "Interstitial impression recorded")
            }
        }
    }

    private fun setupRewardedCallback() {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded Ad dismissed, preloading next one")
                rewardedAd = null
                loadRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded failed to show: ${adError.message}")
                rewardedAd = null
                loadRewardedAd()
            }
            
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded shown successfully")
            }
        }
    }

    /**
     * Show interstitial ad if available
     * @return true if ad was shown, false otherwise
     */
    fun showInterstitialAd(activity: Activity): Boolean {
        val ad = interstitialAd
        
        if (ad == null) {
            Log.d(TAG, "Interstitial not ready, attempting to load...")
            loadInterstitialAd(activity)
            return false
        }
        
        Log.d(TAG, "Showing interstitial ad...")
        ad.show(activity)
        return true
    }

    /**
     * Show Rewarded Ad.
     * FAIL-SAFE: If ad is not ready, we immediately grant the reward (FAIL OPEN).
     */
    fun showRewardedAd(activity: Activity, onUserEarnedReward: (Boolean) -> Unit) {
        val ad = rewardedAd
        
        if (ad != null) {
            Log.d(TAG, "Showing rewarded ad...")
            ad.show(activity) { rewardItem ->
                Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
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
     * Show interstitial ad on every user action (e.g., opening a skin).
     * No cooldowns or action counts - immediate display.
     */
    fun onUserAction(activity: Activity) {
        // Skip if ads are disabled for this build
        if (!BuildConfig.ADS_ENABLED) return
        
        Log.d(TAG, "User action triggered, attempting to show interstitial...")
        
        if (showInterstitialAd(activity)) {
            Log.d(TAG, "Interstitial shown successfully")
        } else {
            Log.d(TAG, "Interstitial not available yet")
        }
    }
    
    /**
     * Force reload all ads (useful after connectivity change or for debugging)
     */
    fun forceReloadAds() {
        Log.d(TAG, "Force reloading all ads...")
        interstitialAd = null
        rewardedAd = null
        interstitialRetryAttempt = 0
        rewardedRetryAttempt = 0
        loadInterstitialAd()
        loadRewardedAd()
    }
}
