package com.functions.goaltribe


import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TribeAdapter(
    private val tribeList: MutableList<Tribe>,
    private val onClick: (Tribe) -> Unit
) : RecyclerView.Adapter<TribeAdapter.TribeViewHolder>() {

    inner class TribeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgTribeAvatar: ImageView = itemView.findViewById(R.id.imgTribeAvatar)
        val tvTribeName: TextView = itemView.findViewById(R.id.tvTribeName)
        val tvTribeCategory: TextView = itemView.findViewById(R.id.tvTribeCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TribeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tribe_avatar, parent, false)
        return TribeViewHolder(view)
    }

    override fun onBindViewHolder(holder: TribeViewHolder, position: Int) {
        val tribe = tribeList[position]
        holder.tvTribeName.text = tribe.name
        holder.tvTribeCategory.text = tribe.category

        // Decode Base64 avatar
        tribe.avatarBase64?.let {
            try {
                val decoded = Base64.decode(it, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.size)
                holder.imgTribeAvatar.setImageBitmap(bitmap)
            } catch (_: Exception) {
                holder.imgTribeAvatar.setImageResource(R.drawable.ic_avatar)
            }
        } ?: holder.imgTribeAvatar.setImageResource(R.drawable.ic_avatar)

        holder.itemView.setOnClickListener {
            onClick(tribe)
        }
    }

    override fun getItemCount(): Int = tribeList.size

    fun updateList(newList: List<Tribe>) {
        tribeList.clear()
        tribeList.addAll(newList)
        notifyDataSetChanged()
    }
}
