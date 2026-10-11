
package com.zizzverse.zizzchat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment

class CallActivity : AppCompatActivity() {

    private var callScreenOpened = false

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            val audioGranted =
                result[Manifest.permission.RECORD_AUDIO] == true ||
                hasPermission(Manifest.permission.RECORD_AUDIO)

            val needsCamera =
                intent.getStringExtra(EXTRA_CALL_TYPE) != TYPE_VOICE

            val cameraGranted =
                !needsCamera ||
                result[Manifest.permission.CAMERA] == true ||
                hasPermission(Manifest.permission.CAMERA)

            if (audioGranted && cameraGranted) {
                openCallScreen()
            } else {
                Toast.makeText(
                    this,
                    "Call করতে প্রয়োজনীয় permission দিন",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callType =
            intent.getStringExtra(EXTRA_CALL_TYPE) ?: TYPE_VOICE

        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )

        if (callType == TYPE_VIDEO) {
            permissions.add(Manifest.permission.CAMERA)
        }

        val missingPermissions = permissions.filter {
            !hasPermission(it)
        }

        if (missingPermissions.isEmpty()) {
            openCallScreen()
        } else {
            permissionLauncher.launch(
                missingPermissions.toTypedArray()
            )
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openCallScreen() {
        if (callScreenOpened || isFinishing || isDestroyed) return

        val currentUserId =
            FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

        val currentUserName =
            intent.getStringExtra(EXTRA_USER_NAME).orEmpty()

        val callId =
            intent.getStringExtra(EXTRA_CALL_ID).orEmpty()

        val callType =
            intent.getStringExtra(EXTRA_CALL_TYPE) ?: TYPE_VOICE

        if (currentUserId.isBlank() ||
            currentUserName.isBlank() ||
            callId.isBlank()
        ) {
            showError("Call information missing")
            return
        }

        if (BuildConfig.ZEGO_APP_SIGN.isBlank()) {
            showError("Call configuration missing")
            return
        }

        try {
            val container = FrameLayout(this).apply {
                id = View.generateViewId()
            }

            setContentView(container)

            val config = if (callType == TYPE_VOICE) {
                ZegoUIKitPrebuiltCallConfig.oneOnOneVoiceCall()
            } else {
                ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall()
            }

            val fragment = ZegoUIKitPrebuiltCallFragment.newInstance(
                BuildConfig.ZEGO_APP_ID,
                BuildConfig.ZEGO_APP_SIGN,
                currentUserId,
                currentUserName,
                callId,
                config
            )

            callScreenOpened = true

            supportFragmentManager.beginTransaction()
                .replace(container.id, fragment)
                .commit()
} catch (e: Exception) {
    callScreenOpened = false
    Log.e("ZizzChatCall", "Could not open call screen", e)

    Toast.makeText(
        this,
        "Call error: ${e.message ?: e.javaClass.simpleName}",
        Toast.LENGTH_LONG
    ).show()

    finish()
}
        
    }

    private fun showError(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
        finish()
    }

    companion object {
        const val EXTRA_USER_NAME = "extra_user_name"
        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_CALL_TYPE = "extra_call_type"

        const val TYPE_VOICE = "voice"
        const val TYPE_VIDEO = "video"
    }
}
