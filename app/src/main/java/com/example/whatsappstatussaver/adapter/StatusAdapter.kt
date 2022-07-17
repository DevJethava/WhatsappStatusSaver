package com.example.whatsappstatussaver.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.whatsappstatussaver.databinding.RawStatusItemBinding
import com.example.whatsappstatussaver.model.FileModel
import java.io.File

class StatusAdapter(
    private val context: Context,
    private var list: MutableList<FileModel>,
    private val itemClick: (FileModel, Int) -> Unit
) :
    RecyclerView.Adapter<StatusAdapter.MyViewHolder>() {

    fun addList(mList: MutableList<FileModel>) {
        this.list.addAll(mList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            RawStatusItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.binding.cardDownload.isVisible = list[position].fileUri.endsWith(".mp4")

        Glide.with(context).load(Uri.parse(list[position].fileUri)).into(holder.binding.ivStatus)

        holder.binding.cardDownload.setOnClickListener {
            itemClick.invoke(list[position], position)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    inner class MyViewHolder(val binding: RawStatusItemBinding) :
        RecyclerView.ViewHolder(binding.root)
}