package com.devstormtech.toe3skins

import android.app.Activity
import android.content.Context
import android.util.Log
import com.devstormtech.toe3skins.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

object ConsentManager {
    private const val TAG = "ConsentManager"
    private val isMobileAdsInitializeCalled = java.util.concurrent.atomic.AtomicBoolean(false)

    /**
     * Helper variable to determine if the app can request ads.
     */
    val canRequestAds: Boolean
        get() = UserMessagingPlatform.getConsentInformation(context).canRequestAds()
        
    private lateinit var consentInformation: ConsentInformation
    private lateinit var context: Context

    fun gatherConsent(
        activity: Activity,
        onConsentGathered: (Boolean) -> Unit
    ) {
        context = activity.applicationContext
        consentInformation = UserMessagingPlatform.getConsentInformation(context)

        // DEBUGGING: Forced Geography for Testing (Remove for production!)
        // val debugSettings = ConsentDebugSettings.Builder(context)
        //     .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
        //     .addTestDeviceHashedId("TEST-DEVICE-HASHED-ID")
        //     .build()

        val paramsBuilder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        if (BuildConfig.DEBUG) {
            val debugSettings = ConsentDebugSettings.Builder(context)
               .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
               // .addTestDeviceHashedId("TEST-DEVICE-HASHED-ID") // Requires actual ID from Logcat
               .build()
            paramsBuilder.setConsentDebugSettings(debugSettings)
        }

        val params = paramsBuilder.build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    activity
                ) { formError ->
                    // Consent gathering failed.
                    if (formError != null) {
                        Log.w(TAG, String.format("%s: %s", formError.errorCode, formError.message))
                    }

                    // Consent has been gathered.
                    if (canRequestAds) {
                        initializeMobileAdsSdk(onConsentGathered)
                    } else {
                        // Consent not granted or not required yet, but flow finished.
                        // We still callback to allow app to proceed, but ads won't load if !canRequestAds
                        onConsentGathered(false)
                    }
                }
            },
            { error ->
                Log.w(TAG, "requestConsentInfoUpdate failed: ${error.message}")
                onConsentGathered(false)
            }
        )
        
        // Check if we can request ads immediately (e.g. returning user)
        if (canRequestAds) {
             initializeMobileAdsSdk(onConsentGathered)
        }
    }

    private fun initializeMobileAdsSdk(onConsentGathered: (Boolean) -> Unit) {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }
        // Notify the caller that they can initialize ads now
        onConsentGathered(true)
    }
}
