package com.simats.endecryption

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.simats.endecryption.databinding.ItemSavedFileBinding

class SavedFilesAdapter(private val savedFiles: List<SavedFile>) :
    RecyclerView.Adapter<SavedFilesAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSavedFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(savedFiles[position])
    }

    override fun getItemCount() = savedFiles.size

    inner class ViewHolder(private val binding: ItemSavedFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(savedFile: SavedFile) {
            binding.fileNameText.text = savedFile.name
            binding.fileSizeText.text = savedFile.size.toString()
        }
    }
}
