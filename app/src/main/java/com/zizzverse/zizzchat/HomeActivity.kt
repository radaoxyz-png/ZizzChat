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

class HomeActivity : Activity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val bg = Color.rgb(10, 16, 32)
    private val card = Color.rgb(22, 32, 53)
    private val blue = Color.rgb(37, 99, 235)
    private val white = Color.WHITE
    private val muted = Color.rgb(154, 170, 196)

    private lateinit var root: LinearLayout
    private lateinit var friendsLayout: LinearLayout

    private var incomingListener: ListenerRegistration? = null
    private var outgoingListener: ListenerRegistration? = null

    private val incomingFriends =
        linkedMapOf<String, Pair<String, String>>()

    private val outgoingFriends =
        linkedMapOf<String, Pair<String, String>>()

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    private fun makeText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (auth.currentUser == null) {
            openLogin()
            return
        }

        window.statusBarColor = bg
        window.navigationBarColor = card

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        setContentView(root)

        // HEADER
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            setBackgroundColor(card)
        }

        root.addView(header)

        val logo = makeText("Z", 24f, white, true).apply {
            gravity = Gravity.CENTER
            background = rounded(blue, 14)
        }

        header.addView(logo, LinearLayout.LayoutParams(dp(46), dp(46)))

        val titleColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        titleColumn.addView(makeText("ZizzChat", 21f, white, true))
        titleColumn.addView(
            makeText("Private conversations", 12f, muted)
        )

        header.addView(
            titleColumn,
            LinearLayout.LayoutParams(0, -2, 1f)
        )

        // FRIENDS LIST AREA
        val content = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
        }

        root.addView(
            content,
            LinearLayout.LayoutParams( -1, 0, 1f)
        )

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(20))
        }

        content.addView(page)

        page.addView(
            makeText("Your Friends", 22f, white, true)
        )

        page.addView(
            makeText(
                "Select a friend to open a private chat.",
                13f,
                muted
            ).apply {
                setPadding(0, dp(6), 0, dp(16))
            }
        )

        friendsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        page.addView(friendsLayout)
        showInfo("Loading friends...")

        // BOTTOM NAVIGATION
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(8), dp(4), dp(8))
            setBackgroundColor(card)
        }

        root.addView(
            nav,
            LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        addNavItem(nav, "⌂", "Home") {
            // Already on Home
            loadFriends()
        }

        addNavItem(nav, "♧", "Friends") {
            startActivity(Intent(this, FriendsActivity::class.java))
        }

        addNavItem(nav, "♧", "Notification") {
            startActivity(
                Intent(this, FriendRequestsActivity::class.java)
            )
        }

        addNavItem(nav, "●", "Profile") {
            showProfile()
        }

        loadFriends()
    }

    private fun addNavItem(
        nav: LinearLayout,
        symbol: String,
        label: String,
        action: () -> Unit
    ) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(2), dp(5), dp(2), dp(5))
            setOnClickListener { action() }
        }

        item.addView(
            makeText(
                symbol,
                23f,
                if (label == "Home") Color.rgb(96, 165, 250) else muted,
                true
            ).apply { gravity = Gravity.CENTER }
        )

        item.addView(
            makeText(
                label,
                10f,
                if (label == "Home") Color.rgb(96, 165, 250) else muted
            ).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(3), 0, 0)
            }
        )

        nav.addView(
            item,
            LinearLayout.LayoutParams(0, -2, 1f)
        )
    }

    private fun loadFriends() {
        val uid = auth.currentUser?.uid ?: return

        incomingListener?.remove()
        outgoingListener?.remove()

        incomingListener = db.collection("friendRequests")
            .whereEqualTo("receiverUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    showInfo("Could not load friends. Check connection and Firestore Rules.")
                    return@addSnapshotListener
                }

                incomingFriends.clear()

                for (doc in snapshot.documents) {
                    if (doc.getString("status") != "accepted") continue

                    val friendUid = doc.getString("senderUid") ?: continue
                    val name = doc.getString("senderName") ?: "User"
                    val email = doc.getString("senderEmail") ?: ""

                    incomingFriends[friendUid] = name to email
                }

                renderFriends()
            }

        outgoingListener = db.collection("friendRequests")
            .whereEqualTo("senderUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    showInfo("Could not load friends. Check connection and Firestore Rules.")
                    return@addSnapshotListener
                }

                outgoingFriends.clear()

                for (doc in snapshot.documents) {
                    if (doc.getString("status") != "accepted") continue

                    val friendUid = doc.getString("receiverUid") ?: continue
                    val name = doc.getString("receiverName") ?: "User"
                    val email = doc.getString("receiverEmail") ?: ""

                    outgoingFriends[friendUid] = name to email
                }

                renderFriends()
            }
    }

    private fun renderFriends() {
        friendsLayout.removeAllViews()

        val all = linkedMapOf<String, Pair<String, String>>()
        all.putAll(incomingFriends)
        all.putAll(outgoingFriends)

        if (all.isEmpty()) {
            showInfo("No friends yet. Open Friends to find people.")
            return
        }

        all.forEach { (friendUid, friend) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                background = rounded(card, 16)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val intent = Intent(
                        this@HomeActivity,
                        ChatActivity::class.java
                    )
                    intent.putExtra("friendUid", friendUid)
                    intent.putExtra("friendName", friend.first)
                    startActivity(intent)
                }
            }

            val avatar = makeText(
                friend.first.take(1).uppercase(),
                19f,
                white,
                true
            ).apply {
                gravity = Gravity.CENTER
                background = rounded(blue, 28)
            }

            row.addView(
                avatar,
                LinearLayout.LayoutParams(dp(46), dp(46))
            )

            val details = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, 0, 0)
            }

            details.addView(makeText(friend.first, 15f, white, true))
            details.addView(
                makeText(
                    if (friend.second.isNotBlank()) friend.second
                    else "Tap to chat privately",
                    12f,
                    muted
                ).apply { setPadding(0, dp(4), 0, 0) }
            )

            row.addView(
                details,
                LinearLayout.LayoutParams(0, -2, 1f)
            )

            row.addView(makeText("›", 28f, muted))

            friendsLayout.addView(
                row,
                LinearLayout.LayoutParams(-1, -2).apply {
                    bottomMargin = dp(10)
                }
            )
        }
    }

    private fun showInfo(message: String) {
        if (!::friendsLayout.isInitialized) return

        friendsLayout.removeAllViews()
        friendsLayout.addView(
            makeText(message, 14f, muted).apply {
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(24), dp(12), dp(24))
            }
        )
    }

    private fun showProfile() {
        val email = auth.currentUser?.email ?: "Chat member"

        val options = arrayOf("Account: $email", "Logout", "Cancel")

        android.app.AlertDialog.Builder(this)
            .setTitle("Profile")
            .setItems(options) { dialog, which ->
                when (which) {
                    1 -> {
                        auth.signOut()
                        openLogin()
                    }
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun openLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        incomingListener?.remove()
        outgoingListener?.remove()
        incomingListener = null
        outgoingListener = null
        super.onDestroy()
    }
}
