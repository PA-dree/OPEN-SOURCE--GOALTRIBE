package com.functions.goaltribe


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class ChallengeAdapter(
    private var challenges: List<Challenge>
) : RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder>() {

    inner class ChallengeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvChallengeTitle)
        val category: TextView = itemView.findViewById(R.id.tvChallengeCategory)
        val description: TextView = itemView.findViewById(R.id.tvChallengeDescription)
        val dates: TextView = itemView.findViewById(R.id.tvChallengeDates)
        val participants: TextView = itemView.findViewById(R.id.tvParticipants)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_challenge, parent, false)
        return ChallengeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        val challenge = challenges[position]
        holder.title.text = challenge.title
        holder.category.text = "Category: ${challenge.category}"
        holder.description.text = challenge.description
        holder.dates.text = "Start: ${challenge.startDate}  •  End: ${challenge.endDate}"
        holder.participants.text = "Participants: ${challenge.participants}"
    }

    override fun getItemCount(): Int = challenges.size

    fun updateList(newList: List<Challenge>) {
        challenges = newList
        notifyDataSetChanged()
    }
}
