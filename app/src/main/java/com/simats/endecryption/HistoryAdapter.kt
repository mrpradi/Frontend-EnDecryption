package com.simats.endecryption

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simats.endecryption.databinding.ItemHistoryBinding

class HistoryAdapter(private var historyItems: MutableList<HistoryItem>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(historyItems[position])
    }

    override fun getItemCount() = historyItems.size

    fun updateData(newHistoryItems: List<HistoryItem>) {
        historyItems.clear()
        historyItems.addAll(newHistoryItems)
        notifyDataSetChanged()
    }

    class ViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryItem) {
            binding.titleText.text = item.title
            binding.subtitleHistory.text = item.subtitle
            binding.valueText.text = item.timestamp ?: item.value
        }
    }
}
