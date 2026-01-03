package com.devstormtech.toe3skins

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Singleton to handle Firebase Analytics events
 */
object AnalyticsManager {
    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(context)
        }
    }

    /**
     * Log when the app is opened
     */
    fun logAppOpen() {
        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.APP_OPEN, null)
    }

    /**
     * Log when a skin is successfully exported to gallery
     */
    fun logSkinExported(truckModel: String) {
        val bundle = Bundle().apply {
            putString("truck_model", truckModel)
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image")
        }
        firebaseAnalytics?.logEvent("skin_exported", bundle)
    }

    /**
     * Log when a project is saved locally
     */
    fun logProjectSaved(truckModel: String) {
        val bundle = Bundle().apply {
            putString("truck_model", truckModel)
        }
        firebaseAnalytics?.logEvent("project_saved", bundle)
    }

    /**
     * Log when a skin is downloaded from the community detail view
     */
    fun logSkinDownloaded(skinTitle: String, truckModel: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.ITEM_NAME, skinTitle)
            putString("truck_model", truckModel)
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "skin")
        }
        firebaseAnalytics?.logEvent("skin_downloaded", bundle)
    }

    /**
     * Log screen views manually if needed (Firebase does this automatically mostly, but good for fragments)
     */
    fun logScreenView(screenName: String, screenClass: String) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
    }
}
