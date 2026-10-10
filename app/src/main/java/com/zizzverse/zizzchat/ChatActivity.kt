package com.zizzverse.zizzchat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

class ChatAdapter(
    private val messages: List<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val row: LinearLayout =
            itemView.findViewById(R.id.messageRow)

        val bubble: LinearLayout =
            itemView.findViewById(R.id.messageBubble)

        val messageText: TextView =
            itemView.findViewById(R.id.messageText)

        val messageTime: TextView =
            itemView.findViewById(R.id.messageTime)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)

        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MessageViewHolder,
        position: Int
    ) {
        val message = messages[position]
        val currentUid = FirebaseAuth.getInstance()
            .currentUser?.uid

        val isMine = message.senderUid == currentUid

        holder.messageText.text = message.text

        holder.row.gravity = if (isMine) {
            Gravity.END
        } else {
            Gravity.START
        }

        holder.bubble.setBackgroundResource(
            if (isMine) {
                R.drawable.message_out_bg
            } else {
                R.drawable.message_in_bg
            }
        )

        val date = message.timestamp?.toDate()

        holder.messageTime.text = if (date != null) {
            SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(date) + if (isMine) "  ✓✓" else ""
        } else {
            ""
        }

        holder.messageTime.setTextColor(
            if (isMine) {
                0xFF9DE3D2.toInt()
            } else {
                0xFFAEB7BC.toInt()
            }
        )
    }

    override fun getItemCount(): Int = messages.size
}
