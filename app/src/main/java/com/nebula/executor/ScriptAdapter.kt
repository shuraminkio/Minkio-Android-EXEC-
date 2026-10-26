package com.nebula.executor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScriptAdapter(
    private var scripts: List<ScriptManager.Script>,
    private val onLoadClick: (ScriptManager.Script) -> Unit,
    private val onExecuteClick: (ScriptManager.Script) -> Unit
) : RecyclerView.Adapter<ScriptAdapter.ScriptViewHolder>() {

    inner class ScriptViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvScriptName)
        val tvDesc: TextView = itemView.findViewById(R.id.tvScriptDesc)
        val btnRun: TextView = itemView.findViewById(R.id.btnRunScript)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScriptViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_script, parent, false)
        return ScriptViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScriptViewHolder, position: Int) {
        val script = scripts[position]
        holder.tvName.text = script.name
        holder.tvDesc.text = script.description

        holder.itemView.setOnClickListener { onLoadClick(script) }
        holder.btnRun.setOnClickListener { onExecuteClick(script) }
    }

    override fun getItemCount(): Int = scripts.size

    fun updateScripts(newScripts: List<ScriptManager.Script>) {
        scripts = newScripts
        notifyDataSetChanged()
    }
}
