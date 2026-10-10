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
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

// ==========================================
// 👥 FRIENDS ACTIVITY — SEARCH + ADD FRIEND
// ==========================================

class FriendsActivity : Activity() {

    // 🔥 FIREBASE AUTHENTICATION
    private val auth by lazy { FirebaseAuth.getInstance() }

    // 🔥 FIRESTORE DATABASE
    private val db by lazy { FirebaseFirestore.getInstance() }

    // 🎨 UI COLORS
    private val bg = Color.rgb(10, 16, 32)
    private val card = Color.rgb(22, 32, 53)
    private val blue = Color.rgb(37, 99, 235)
    private val white = Color.WHITE
    private val muted = Color.rgb(154, 170, 196)
    private val green = Color.rgb(34, 197, 94)

    // 🔍 SEARCH INPUT
    private lateinit var searchInput: EditText

    // 📋 SEARCH RESULTS CONTAINER
    private lateinit var resultsLayout: LinearLayout

    // 🔎 SEARCH BUTTON
    private lateinit var searchButton: TextView

    // 📏 DP CONVERTER
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    // 🎨 ROUNDED BACKGROUND
    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }
    }

    // 📝 TEXT DESIGN
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

    // 🔵 BUTTON DESIGN
    private fun makeButton(
        value: String,
        action: () -> Unit
    ): TextView {
        return makeText(value, 14f, white, true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(blue, 12)

            setOnClickListener { action() }
        }
    }

    // 🖥️ FRIEND SEARCH PAGE UI
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
            setBackgroundColor(bg)
            setPadding(dp(18), dp(16), dp(18), dp(12))
        }

        setContentView(root)

        // 🔙 HEADER + BACK BUTTON
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val backButton = makeText("‹", 36f, white, true).apply {
            setPadding(dp(6), 0, dp(16), 0)

            setOnClickListener {
                finish()
            }
        }

        header.addView(backButton)

        header.addView(
            makeText("Find Friends", 25f, white, true),
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        root.addView(header)

        // 📝 PAGE SUBTITLE
        root.addView(
            makeText(
                "Search by username, email or UID",
                14f,
                muted
            ).apply {
                setPadding(0, dp(8), 0, dp(20))
            }
        )

        // 🔍 SEARCH BAR ROW
        val searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        root.addView(searchRow)

        // 🔎 SEARCH INPUT FIELD
        searchInput = EditText(this).apply {
            hint = "Username / Gmail / UID"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 14f
            setSingleLine(true)

            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(card, 14)

            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH

            setOnEditorActionListener { _, actionId, _ ->
                if (
                    actionId ==
                    android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
                ) {
                    searchUsers()
                    true
                } else {
                    false
                }
            }
        }

        searchRow.addView(
            searchInput,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        // 🔵 SEARCH BUTTON
        searchButton = makeButton("Search") {
            searchUsers()
        }

        searchRow.addView(
            searchButton,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(8), 0, 0, 0)
            }
        )

        // 📋 RESULTS SCROLL VIEW
        val resultsScroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
        }

        root.addView(
            resultsScroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            ).apply {
                topMargin = dp(20)
            }
        )

        // 📋 RESULTS CONTAINER
        resultsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        resultsScroll.addView(resultsLayout)

        // 👋 INITIAL EMPTY STATE
        showInfo("Search for a friend to get started.")
    }

    // 🔍 SEARCH USER BY USERNAME / EMAIL / UID
    private fun searchUsers() {
        val queryText = searchInput.text.toString().trim()

        // ⚠️ EMPTY SEARCH CHECK
        if (queryText.isBlank()) {
            searchInput.error = "Enter username, email or UID"
            return
        }

        // ⌨️ HIDE KEYBOARD
        val imm = getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager

        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)

        // ⏳ SEARCHING STATE
        searchButton.isEnabled = false
        searchButton.text = "..."

        resultsLayout.removeAllViews()
        showInfo("Searching...")

        // 🔍 USERNAME SEARCH
        // usernameLower is saved during sign-up
        val normalized = queryText.removePrefix("@").lowercase()

        db.collection("users")
            .whereEqualTo("usernameLower", normalized)
            .limit(10)
            .get()
            .addOnSuccessListener { usernameResults ->

                // 📧 EMAIL SEARCH — if username did not match
                if (!usernameResults.isEmpty) {
                    showSearchResults(usernameResults.documents)
                } else {
                    searchByEmailOrUid(queryText)
                }
            }
            .addOnFailureListener { error ->
                searchButton.isEnabled = true
                searchButton.text = "Search"

                showInfo(
                    "Search failed: " +
                        (error.localizedMessage ?: "Check connection")
                )
            }
    }

    // 📧 SEARCH BY EMAIL, THEN UID
    private fun searchByEmailOrUid(queryText: String) {

        db.collection("users")
            .whereEqualTo("email", queryText.lowercase())
            .limit(10)
            .get()
            .addOnSuccessListener { emailResults ->

                if (!emailResults.isEmpty) {
                    showSearchResults(emailResults.documents)
                } else {
                    // 🔎 UID SEARCH
                    db.collection("users")
                        .document(queryText)
                        .get()
                        .addOnSuccessListener { document ->

                            searchButton.isEnabled = true
                            searchButton.text = "Search"

                            if (document.exists()) {
                                showSearchResults(listOf(document))
                            } else {
                                showInfo("No user found.")
                            }
                        }
                        .addOnFailureListener {
                            searchButton.isEnabled = true
                            searchButton.text = "Search"
                            showInfo("Could not search. Try again.")
                        }
                }
            }
            .addOnFailureListener {
                searchButton.isEnabled = true
                searchButton.text = "Search"
                showInfo("Could not search. Try again.")
            }
    }

    // 👤 DISPLAY SEARCH RESULTS
    private fun showSearchResults(
        documents: List<com.google.firebase.firestore.DocumentSnapshot>
    ) {
        resultsLayout.removeAllViews()

        searchButton.isEnabled = true
        searchButton.text = "Search"

        val currentUid = auth.currentUser?.uid

        // 🚫 HIDE YOUR OWN ACCOUNT FROM RESULTS
        val users = documents.filter {
            it.getString("uid") != currentUid
        }

        if (users.isEmpty()) {
            showInfo("No other users found.")
            return
        }

        // 👥 SHOW EACH MATCHED USER
        users.forEach { document ->

            val uid = document.getString("uid") ?: document.id
            val username = document.getString("username") ?: "User"
            val email = document.getString("email") ?: ""

            // 📦 USER RESULT CARD
            val userCard = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(14), dp(14), dp(14))
                background = rounded(card, 16)
            }

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }

            resultsLayout.addView(userCard, params)

            // 👤 AVATAR PLACEHOLDER
            val avatar = makeText(
                username.take(1).uppercase(),
                20f,
                white,
                true
            ).apply {
                gravity = Gravity.CENTER
                background = rounded(blue, 30)
            }

            userCard.addView(
                avatar,
                LinearLayout.LayoutParams(dp(48), dp(48))
            )

            // 📝 USER DETAILS
            val detailColumn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(8), 0)
            }

            detailColumn.addView(
                makeText(username, 16f, white, true)
            )

            detailColumn.addView(
                makeText(
                    if (email.isNotBlank()) email else "UID: $uid",
                    11f,
                    muted
                )
            )

            userCard.addView(
                detailColumn,
                LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            // ➕ ADD FRIEND BUTTON
            val addButton = makeButton("Add Friend") {
                sendFriendRequest(uid, username, it = null)
            }

            userCard.addView(addButton)
        }
    }

    // 📤 SEND FRIEND REQUEST TO FIRESTORE
    private fun sendFriendRequest(
        receiverUid: String,
        receiverName: String,
        it: Any? = null
    ) {
        val currentUser = auth.currentUser ?: return

        if (receiverUid == currentUser.uid) {
            Toast.makeText(
                this,
                "You cannot add yourself",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // 🔑 DETERMINISTIC REQUEST ID — avoid duplicate requests
        val requestId = "${currentUser.uid}_$receiverUid"
        val requestRef = db.collection("friendRequests")
            .document(requestId)

        // 🔍 CHECK WHETHER REQUEST ALREADY EXISTS
        requestRef.get()
            .addOnSuccessListener { existing ->

                if (existing.exists()) {
                    Toast.makeText(
                        this,
                        "A request already exists",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                // 📦 FRIEND REQUEST DATA
                val data = hashMapOf(
                    "senderUid" to currentUser.uid,
                    "senderEmail" to (currentUser.email ?: ""),
                    "senderName" to (currentUser.displayName ?: currentUser.email ?: "User"),
                    "receiverUid" to receiverUid,
                    "receiverName" to receiverName,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis()
                )

                // 🔥 SAVE FRIEND REQUEST
                requestRef.set(data)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Friend request sent!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(
                            this,
                            error.localizedMessage
                                ?: "Could not send request",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    error.localizedMessage ?: "Could not check request",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // 📝 SHOW MESSAGE IN RESULTS AREA
    private fun showInfo(message: String) {
        resultsLayout.removeAllViews()

        resultsLayout.addView(
            makeText(message, 14f, muted).apply {
                gravity = Gravity.CENTER
                setPadding(dp(12), dp(24), dp(12), dp(24))
            }
        )
    }
}
