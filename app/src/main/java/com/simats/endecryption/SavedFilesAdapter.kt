package com.simats.endecryption

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.simats.endecryption.databinding.ItemSavedFileBinding
import java.text.SimpleDateFormat
import java.util.*

class SavedFilesAdapter(private var savedFiles: List<SavedFile>) :
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

    fun updateData(newFiles: List<SavedFile>) {
        savedFiles = newFiles
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemSavedFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(savedFile: SavedFile) {
            binding.fileNameText.text = savedFile.name
            
            val sizeKb = savedFile.size / 1024.0
            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(savedFile.lastModified))
            binding.fileSizeText.text = String.format("%.2f KB • %s", sizeKb, dateStr)

            binding.moreOptionsButton.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add("Decrypt")
                
                popup.setOnMenuItemClickListener { item ->
                    when (item.title) {
                        "Decrypt" -> {
                            val intent = android.content.Intent(view.context, DecryptionActivity::class.java)
                            intent.putExtra("FILE_ID", savedFile.id)
                            intent.putExtra("FILE_NAME", savedFile.name)
                            view.context.startActivity(intent)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }
    }
}
