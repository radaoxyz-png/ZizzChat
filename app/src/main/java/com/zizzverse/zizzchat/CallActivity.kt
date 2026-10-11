package com.zizzverse.zizzchat

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallFragment
import com.zegocloud.uikit.prebuilt.call.ZegoUIKitPrebuiltCallConfig

class CallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUserId = com.google.firebase.auth.FirebaseAuth
            .getInstance().currentUser?.uid.orEmpty()

        val currentUserName = intent
            .getStringExtra(EXTRA_USER_NAME).orEmpty()

        val callId = intent
            .getStringExtra(EXTRA_CALL_ID).orEmpty()

        val callType = intent
            .getStringExtra(EXTRA_CALL_TYPE).orEmpty()

        if (currentUserId.isBlank() ||
            currentUserName.isBlank() ||
            callId.isBlank()
        ) {
            Toast.makeText(
                this,
                "Call information missing",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        if (BuildConfig.ZEGO_APP_SIGN.isBlank()) {
            Toast.makeText(
                this,
                "Call configuration missing",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        val container = FrameLayout(this).apply {
            id = View.generateViewId()
        }

        setContentView(container)

        val config =
            if (callType == TYPE_VOICE) {
                ZegoUIKitPrebuiltCallConfig.oneOnOneVoiceCall()
            } else {
                ZegoUIKitPrebuiltCallConfig.oneOnOneVideoCall()
            }

        val fragment =
            ZegoUIKitPrebuiltCallFragment.newInstance(
                BuildConfig.ZEGO_APP_ID,
                BuildConfig.ZEGO_APP_SIGN,
                currentUserId,
                currentUserName,
                callId,
                config
            )

        supportFragmentManager
            .beginTransaction()
            .replace(container.id, fragment)
            .commit()
    }

    companion object {
        const val EXTRA_USER_NAME = "extra_user_name"
        const val EXTRA_CALL_ID = "extra_call_id"
        const val EXTRA_CALL_TYPE = "extra_call_type"

        const val TYPE_VOICE = "voice"
        const val TYPE_VIDEO = "video"
    }
}
