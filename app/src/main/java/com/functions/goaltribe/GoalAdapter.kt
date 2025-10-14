package com.functions.goaltribe

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GoalAdapter(private var goals: MutableList<Goal>) :
    RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    // A constant is recommended for maximum progress if you're not using it elsewhere
    private val MAX_PROGRESS = 100

    inner class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Assuming R.id.tv_goal_title, R.id.tv_goal_percentage, and R.id.goal_progress_bar exist
        val title: TextView = itemView.findViewById(R.id.tv_goal_title)
        val progressText: TextView = itemView.findViewById(R.id.tv_goal_percentage)
        val progressBar: ProgressBar = itemView.findViewById(R.id.goal_progress_bar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal, parent, false)
        return GoalViewHolder(view)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]

        // FIX: Use 'goalName' field as defined in the Goal data class
        holder.title.text = goal.goalName

        holder.progressText.text = "${goal.progress}%"

        // Ensure the ProgressBar maximum is set (usually 100 in XML, but good to ensure here)
        holder.progressBar.max = MAX_PROGRESS
        holder.progressBar.progress = goal.progress

        // OPTIONAL: Add an OnClickListener for item interaction (e.g., viewing goal details)
        // holder.itemView.setOnClickListener {
        //     // Handle click logic here
        // }
    }

    override fun getItemCount(): Int = goals.size

    // Use DiffUtil for better performance in larger lists (Recommended)
    fun updateGoals(newGoals: List<Goal>) {
        // While DiffUtil is preferred, this implementation is functionally correct
        goals.clear()
        goals.addAll(newGoals)
        notifyDataSetChanged()
    }
}