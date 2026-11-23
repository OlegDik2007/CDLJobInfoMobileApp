package info.cdljob.driverhub

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseBootstrap {
    fun initialize(context: Context): Boolean {
        if (FirebaseApp.getApps(context).isNotEmpty()) return true

        val projectId = BuildConfig.FIREBASE_PROJECT_ID.trim()
        val applicationId = BuildConfig.FIREBASE_APPLICATION_ID.trim()
        val apiKey = BuildConfig.FIREBASE_API_KEY.trim()
        val senderId = BuildConfig.FIREBASE_GCM_SENDER_ID.trim()

        if (projectId.isEmpty() || applicationId.isEmpty() || apiKey.isEmpty() || senderId.isEmpty()) {
            // Do not silently claim remote push is active in a build without
            // the four Firebase client configuration values.
            Log.w("CDLPush", "Firebase client configuration missing; background local road alerts remain available")
            return false
        }

        return try {
            val options = FirebaseOptions.Builder()
                .setProjectId(projectId)
                .setApplicationId(applicationId)
                .setApiKey(apiKey)
                .setGcmSenderId(senderId)
                .build()
            FirebaseApp.initializeApp(context.applicationContext, options)
            true
        } catch (e: RuntimeException) {
            // Never log API keys or tokens.
            Log.e("CDLPush", "Firebase initialization failed", e)
            false
        }
    }
}
