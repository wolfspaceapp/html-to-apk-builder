package com.htmltoapk.builder.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.htmltoapk.builder.HtmlToApkApplication
import com.htmltoapk.builder.R
import com.htmltoapk.builder.data.ProjectEntity
import com.htmltoapk.builder.databinding.ActivityMainBinding
import com.htmltoapk.builder.databinding.ItemProjectBinding
import com.htmltoapk.builder.model.Project
import com.htmltoapk.builder.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val adapter = ProjectAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        binding.projectsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }
    }

    private fun setupObservers() {
        viewModel.projects.observe(this) { projects ->
            adapter.submitList(projects)
            binding.emptyView.isVisible = projects.isEmpty()
            binding.projectsRecyclerView.isVisible = projects.isNotEmpty()
        }

        viewModel.buildState.observe(this) { state ->
            when (state) {
                is MainViewModel.BuildState.Building -> {
                    binding.progressBar.isVisible = true
                    binding.buildProgressText.text = state.message
                }
                is MainViewModel.BuildState.Success -> {
                    binding.progressBar.isVisible = false
                    Toast.makeText(this, "Build successful: ${state.outputFile.name}", Toast.LENGTH_LONG).show()
                }
                is MainViewModel.BuildState.Error -> {
                    binding.progressBar.isVisible = false
                    Toast.makeText(this, "Build failed: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabNewProject.setOnClickListener {
            val intent = Intent(this, ProjectEditorActivity::class.java)
            startActivity(intent)
        }

        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    inner class ProjectAdapter :
        androidx.recyclerview.widget.ListAdapter<ProjectEntity, ProjectAdapter.ProjectViewHolder>(
            ProjectDiffCallback()
        ) {

        inner class ProjectViewHolder(private val binding: ItemProjectBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

            fun bind(project: ProjectEntity) {
                binding.tvProjectName.text = project.name
                binding.tvPackageName.text = project.packageName
                binding.tvVersion.text = "v${project.versionName} (${project.versionCode})"

                binding.root.setOnClickListener {
                    val intent = Intent(itemView.context, BuildActivity::class.java).apply {
                        putExtra("project_id", project.id)
                    }
                    startActivity(intent)
                }

                binding.root.setOnLongClickListener {
                    showProjectMenu(project)
                    true
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
            val binding = ItemProjectBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ProjectViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        private fun showProjectMenu(project: ProjectEntity) {
            val menu = androidx.appcompat.widget.PopupMenu(this@MainActivity, binding.fabNewProject)
            menu.menu.add("Edit").setOnMenuItemClickListener {
                val intent = Intent(this@MainActivity, ProjectEditorActivity::class.java).apply {
                    putExtra("project_id", project.id)
                }
                startActivity(intent)
                true
            }
            menu.menu.add("Duplicate").setOnMenuItemClickListener {
                duplicateProject(project)
                true
            }
            menu.menu.add("Delete").setOnMenuItemClickListener {
                deleteProject(project)
                true
            }
            menu.show()
        }

        private fun duplicateProject(project: ProjectEntity) {
            lifecycleScope.launch {
                val newProject = project.copy(id = 0, name = "${project.name} (Copy)", createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
                viewModel.insertProject(newProject)
            }
        }

        private fun deleteProject(project: ProjectEntity) {
            androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("Delete Project")
                .setMessage("Are you sure you want to delete '${project.name}'?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        viewModel.deleteProject(project.id)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    class ProjectDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<ProjectEntity>() {
        override fun areItemsTheSame(oldItem: ProjectEntity, newItem: ProjectEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: ProjectEntity, newItem: ProjectEntity): Boolean = oldItem == newItem
    }
}