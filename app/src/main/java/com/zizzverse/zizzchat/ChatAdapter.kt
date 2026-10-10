package com.zizzverse.zizzchat

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class ChatAdapter(
    private val messages: List<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

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
        val isMine = message.senderUid ==
            com.google.firebase.auth.FirebaseAuth
                .getInstance().currentUser?.uid

        holder.messageText.text = message.text

        val params = holder.messageText.layoutParams as
            LinearLayout.LayoutParams

        params.gravity = if (isMine) {
            Gravity.END
        } else {
            Gravity.START
        }

        holder.messageText.layoutParams = params

        holder.messageText.setBackgroundColor(
            if (isMine) 0xFFDCE8FF.toInt()
            else 0xFFFFFFFF.toInt()
        )

        holder.messageText.setTextColor(0xFF172033.toInt())

        val time = message.timestamp?.toDate()

        holder.messageTime.text = if (time != null) {
            SimpleDateFormat("hh:mm a", Locale.getDefault())
                .format(time)
        } else {
            ""
        }

        val timeParams = holder.messageTime.layoutParams as
            LinearLayout.LayoutParams

        timeParams.gravity = if (isMine) {
            Gravity.END
        } else {
            Gravity.START
        }

        holder.messageTime.layoutParams = timeParams
    }

    override fun getItemCount(): Int = messages.size
}
