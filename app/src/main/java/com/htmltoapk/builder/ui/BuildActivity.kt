package com.htmltoapk.builder.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.htmltoapk.builder.HtmlToApkApplication
import com.htmltoapk.builder.R
import com.htmltoapk.builder.databinding.ActivityBuildBinding
import com.htmltoapk.builder.data.BuildHistoryEntity
import com.htmltoapk.builder.viewmodel.BuildViewModel
import kotlinx.coroutines.launch

class BuildActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBuildBinding
    private val viewModel: BuildViewModel by viewModels()
    private var projectId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBuildBinding.inflate(layoutInflater)
        setContentView(binding.root)

        projectId = intent.getLongExtra("project_id", 0)
        if (projectId == 0) {
            finish()
            return
        }

        setupToolbar()
        setupObservers()
        setupClickListeners()

        viewModel.loadProject(projectId)
        viewModel.loadBuildHistory(projectId)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupObservers() {
        viewModel.project.observe(this) { project ->
            project?.let {
                binding.tvProjectName.text = it.name
                binding.tvPackageName.text = it.packageName
                binding.tvVersion.text = "v${it.versionName} (${it.versionCode})"
                binding.tvBuildMode.text = it.buildMode
                binding.tvOutputFormat.text = it.outputFormat.uppercase()
            }
        }

        viewModel.buildHistory.observe(this) { history ->
            binding.rvBuildHistory.adapter = BuildHistoryAdapter(history)
        }

        viewModel.buildState.observe(this) { state ->
            when (state) {
                is BuildViewModel.BuildState.Building -> {
                    binding.progressLayout.visibility = View.VISIBLE
                    binding.tvBuildStatus.text = state.message
                    binding.progressBar.isIndeterminate = true
                    binding.btnBuild.isEnabled = false
                    binding.btnOpenOutput.isEnabled = false
                }
                is BuildViewModel.BuildState.Success -> {
                    binding.progressLayout.visibility = View.GONE
                    binding.btnBuild.isEnabled = true
                    binding.btnOpenOutput.isEnabled = true
                    Toast.makeText(this, "Build successful: ${state.outputFile.name}", Toast.LENGTH_LONG).show()
                }
                is BuildViewModel.BuildState.Error -> {
                    binding.progressLayout.visibility = View.GONE
                    binding.btnBuild.isEnabled = true
                    Toast.makeText(this, "Build failed: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBuild.setOnClickListener {
            viewModel.buildProject(projectId)
        }

        binding.btnOpenOutput.setOnClickListener {
            viewModel.openOutputFile(this)
        }

        binding.btnViewHistory.setOnClickListener {
            // History is already visible
        }
    }

    inner class BuildHistoryAdapter(
        private val history: List<BuildHistoryEntity>
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<BuildHistoryAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
            val tvDate: android.widget.TextView = view.findViewById(R.id.tvHistoryDate)
            val tvStatus: android.widget.TextView = view.findViewById(R.id.tvHistoryStatus)
            val tvFormat: android.widget.TextView = view.findViewById(R.id.tvHistoryFormat)
            val tvDuration: android.widget.TextView = view.findViewById(R.id.tvHistoryDuration)
        }

        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_build_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = history[position]
            holder.tvDate.text = android.text.format.DateFormat.format("MM/dd/yyyy HH:mm", item.createdAt)
            holder.tvStatus.text = if (item.success) "Success" else "Failed"
            holder.tvStatus.setTextColor(
                if (item.success)
                    androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.success_color)
                else
                    androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.error_color)
            )
            holder.tvFormat.text = item.outputFormat.uppercase()
            holder.tvDuration.text = "${(item.buildDurationMs / 1000)}s"
        }

        override fun getItemCount(): Int = history.size
    }
}