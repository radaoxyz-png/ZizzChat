package com.zizzverse.zizzchat

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

// ==========================================
// 📩 FRIEND REQUESTS — ACCEPT / REJECT
// ==========================================

class FriendRequestsActivity : Activity() {

    // 🔥 FIREBASE AUTHENTICATION
    private val auth by lazy { FirebaseAuth.getInstance() }

    // 🔥 FIRESTORE DATABASE
    private val db by lazy { FirebaseFirestore.getInstance() }

    // 🎨 UI COLORS
    private val bg = Color.rgb(10, 16, 32)
    private val card = Color.rgb(22, 32, 53)
    private val blue = Color.rgb(37, 99, 235)
    private val green = Color.rgb(22, 163, 74)
    private val red = Color.rgb(185, 28, 28)
    private val white = Color.WHITE
    private val muted = Color.rgb(154, 170, 196)

    // 📋 REQUESTS LIST
    private lateinit var requestsLayout: LinearLayout

    // 📡 LIVE REQUEST LISTENER
    private var requestListener: ListenerRegistration? = null

    // 📏 DP CONVERTER
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    // 🎨 ROUNDED BACKGROUND
    private fun rounded(color: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    // 📝 TEXT DESIGN
    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
    }

    // 🔵 ACTION BUTTON DESIGN
    private fun makeButton(
        value: String,
        color: Int,
        action: () -> Unit
    ) = makeText(value, 13f, white, true).apply {
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(11), dp(14), dp(11))
        background = rounded(color, 12)
        setOnClickListener { action() }
    }

    // 🖥️ PAGE UI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔐 LOGIN REQUIRED
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        window.statusBarColor = bg
        window.navigationBarColor = bg

        // 📱 MAIN LAYOUT
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(12))
            setBackgroundColor(bg)
        }

        setContentView(root)

        // 🔙 HEADER
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(
            makeText("‹", 36f, white, true).apply {
                setPadding(0, 0, dp(16), 0)
                setOnClickListener { finish() }
            }
        )

        header.addView(
            makeText("Friend Requests", 24f, white, true)
        )

        root.addView(header)

        // 📝 SUBTITLE
        root.addView(
            makeText(
                "Requests sent to you",
                14f,
                muted
            ).apply {
                setPadding(0, dp(8), 0, dp(18))
            }
        )

        // 📜 SCROLL VIEW
        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
        }

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        // 📋 REQUESTS CONTAINER
        requestsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(requestsLayout)

        // 🔥 START LOADING REQUESTS
        listenForRequests()
    }

    // 📥 LISTEN FOR INCOMING FRIEND REQUESTS
    private fun listenForRequests() {
        val uid = auth.currentUser?.uid ?: return

        // 🧹 REMOVE PREVIOUS LISTENER
        requestListener?.remove()

        requestsLayout.removeAllViews()
        showInfo("Loading friend requests...")

        requestListener = db.collection("friendRequests")
            .whereEqualTo("receiverUid", uid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->

                // ❌ LOAD ERROR
                if (error != null) {
                    showInfo(
                        "Could not load requests. Check Firestore Rules."
                    )
                    return@addSnapshotListener
                }

                if (snapshot == null) return@addSnapshotListener

                requestsLayout.removeAllViews()

                // 📭 EMPTY STATE
                if (snapshot.isEmpty) {
                    showInfo("No friend requests yet.")
                    return@addSnapshotListener
                }

                // 👥 DISPLAY EACH REQUEST
                for (document in snapshot.documents) {
                    val requestId = document.id
                    val senderUid =
                        document.getString("senderUid") ?: continue
                    val senderName =
                        document.getString("senderName") ?: "User"
                    val senderEmail =
                        document.getString("senderEmail") ?: ""

                    addRequestCard(
                        requestId,
                        senderUid,
                        senderName,
                        senderEmail
                    )
                }
            }
    }

    // 👤 REQUEST CARD UI
    private fun addRequestCard(
        requestId: String,
        senderUid: String,
        senderName: String,
        senderEmail: String
    ) {
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(card, 16)
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(12)
        }

        requestsLayout.addView(cardLayout, params)

        // 👤 SENDER NAME
        cardLayout.addView(
            makeText(senderName, 17f, white, true)
        )

        // 📧 SENDER EMAIL / UID
        cardLayout.addView(
            makeText(
                if (senderEmail.isNotBlank()) {
                    senderEmail
                } else {
                    "UID: $senderUid"
                },
                12f,
                muted
            ).apply {
                setPadding(0, dp(5), 0, dp(12))
            }
        )

        // 🔘 ACCEPT / REJECT ROW
        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        cardLayout.addView(actions)

        // ✅ ACCEPT BUTTON
        actions.addView(
            makeButton("Accept", green) {
                updateRequestStatus(requestId, "accepted")
            },
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(0, 0, dp(5), 0)
            }
        )

        // ❌ REJECT BUTTON
        actions.addView(
            makeButton("Reject", red) {
                updateRequestStatus(requestId, "rejected")
            },
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(dp(5), 0, 0, 0)
            }
        )
    }

    // 🔄 ACCEPT / REJECT REQUEST
    private fun updateRequestStatus(
        requestId: String,
        newStatus: String
    ) {
        val uid = auth.currentUser?.uid ?: return

        val requestRef = db.collection("friendRequests")
            .document(requestId)

        // 🔍 READ REQUEST BEFORE UPDATING
        requestRef.get()
            .addOnSuccessListener { document ->

                if (!document.exists()) {
                    Toast.makeText(
                        this,
                        "Request no longer exists",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                // 🔐 VERIFY THIS USER IS THE RECIPIENT
                if (
                    document.getString("receiverUid") != uid ||
                    document.getString("status") != "pending"
                ) {
                    Toast.makeText(
                        this,
                        "Request is no longer available",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                // 🔄 UPDATE REQUEST STATUS
                requestRef.update("status", newStatus)
                    .addOnSuccessListener {

                        val message =
                            if (newStatus == "accepted") {
                                "Friend request accepted!"
                            } else {
                                "Friend request rejected."
                            }

                        Toast.makeText(
                            this,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()

                        // ⚠️ Acceptance status alone does not create a
                        // complete Friends list/private chat yet.
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(
                            this,
                            error.localizedMessage
                                ?: "Could not update request",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    error.localizedMessage ?: "Could not open request",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // 📝 EMPTY / ERROR MESSAGE
    private fun showInfo(message: String) {
        requestsLayout.removeAllViews()

        requestsLayout.addView(
            makeText(message, 14f, muted).apply {
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(28), dp(12), dp(28))
            }
        )
    }

    // 🧹 CLEAN UP FIRESTORE LISTENER
    override fun onDestroy() {
        requestListener?.remove()
        requestListener = null
        super.onDestroy()
    }
}
