package com.htmltoapk.builder.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.htmltoapk.builder.HtmlToApkApplication
import com.htmltoapk.builder.R
import com.htmltoapk.builder.data.AppDatabase
import com.htmltoapk.builder.data.KeystoreEntity
import com.htmltoapk.builder.databinding.ActivityKeystoreManagerBinding
import com.htmltoapk.builder.databinding.ItemKeystoreBinding
import com.htmltoapk.builder.viewmodel.KeystoreManagerViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

class KeystoreManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKeystoreManagerBinding
    private val viewModel: KeystoreManagerViewModel by viewModels()
    private val adapter = KeystoreAdapter()

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleFileSelection(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKeystoreManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Keystore Manager"
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun setupRecyclerView() {
        binding.keystoreRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@KeystoreManagerActivity)
            adapter = this@KeystoreManagerActivity.adapter
        }
    }

    private fun setupObservers() {
        viewModel.keystores.observe(this) { keystores ->
            adapter.submitList(keystores)
            binding.emptyView.visibility = if (keystores.isEmpty()) View.VISIBLE else View.GONE
            binding.keystoreRecyclerView.visibility = if (keystores.isNotEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.fabAddKeystore.setOnClickListener {
            showAddKeystoreDialog()
        }
    }

    private fun showAddKeystoreDialog(isEditing: Boolean = false, existingKeystore: KeystoreEntity? = null) {
        val dialogBinding = com.htmltoapk.builder.databinding.DialogKeystoreBinding.inflate(layoutInflater)

        if (isEditing && existingKeystore != null) {
            dialogBinding.etName.setText(existingKeystore.name)
            dialogBinding.etAlias.setText(existingKeystore.alias)
            dialogBinding.etFilePath.setText(existingKeystore.filePath)
            // Don't show passwords for security
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (isEditing) "Edit Keystore" else "Add Keystore")
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEditing) "Update" else "Add") { _, _ ->
                val name = dialogBinding.etName.text.toString().trim()
                val filePath = dialogBinding.etFilePath.text.toString().trim()
                val alias = dialogBinding.etAlias.text.toString().trim()
                val password = dialogBinding.etPassword.text.toString()
                val keyPassword = dialogBinding.etKeyPassword.text.toString()

                if (name.isEmpty() || filePath.isEmpty() || alias.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (isEditing && existingKeystore != null) {
                    val updated = existingKeystore.copy(
                        name = name,
                        filePath = filePath,
                        alias = alias,
                        password = password, // Should be encrypted in production
                        keyPassword = keyPassword
                    )
                    viewModel.updateKeystore(updated)
                } else {
                    val keystore = KeystoreEntity(
                        name = name,
                        filePath = filePath,
                        alias = alias,
                        password = password, // Should be encrypted in production
                        keyPassword = keyPassword
                    )
                    viewModel.insertKeystore(keystore)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()

        dialogBinding.btnBrowseFile.setOnClickListener {
            filePickerLauncher.launch("*/*")
        }
    }

    private fun handleFileSelection(uri: Uri) {
        val path = getPathFromUri(uri)
        // We need to pass this to the dialog - for simplicity use a tag
        // In production, use a proper dialog fragment
    }

    private fun getPathFromUri(uri: Uri): String {
        return uri.toString()
    }

    inner class KeystoreAdapter :
        androidx.recyclerview.widget.ListAdapter<KeystoreEntity, KeystoreAdapter.KeystoreViewHolder>(
            KeystoreDiffCallback()
        ) {

        inner class KeystoreViewHolder(private val binding: ItemKeystoreBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

            fun bind(keystore: KeystoreEntity) {
                binding.tvName.text = keystore.name
                binding.tvAlias.text = "Alias: ${keystore.alias}"
                binding.tvPath.text = keystore.filePath
                binding.chipDefault.isVisible = keystore.isDefault

                binding.root.setOnClickListener {
                    showAddKeystoreDialog(true, keystore)
                }

                binding.root.setOnLongClickListener {
                    showKeystoreMenu(keystore)
                    true
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeystoreViewHolder {
            val binding = ItemKeystoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return KeystoreViewHolder(binding)
        }

        override fun onBindViewHolder(holder: KeystoreViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        private fun showKeystoreMenu(keystore: KeystoreEntity) {
            val menu = androidx.appcompat.widget.PopupMenu(this@KeystoreManagerActivity, binding.fabAddKeystore)
            menu.menu.add("Set as Default").setOnMenuItemClickListener {
                viewModel.setDefaultKeystore(keystore.id)
                true
            }
            menu.menu.add("Delete").setOnMenuItemClickListener {
                deleteKeystore(keystore)
                true
            }
            menu.show()
        }

        private fun deleteKeystore(keystore: KeystoreEntity) {
            MaterialAlertDialogBuilder(this@KeystoreManagerActivity)
                .setTitle("Delete Keystore")
                .setMessage("Are you sure you want to delete '${keystore.name}'?")
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteKeystore(keystore.id)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    class KeystoreDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<KeystoreEntity>() {
        override fun areItemsTheSame(oldItem: KeystoreEntity, newItem: KeystoreEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: KeystoreEntity, newItem: KeystoreEntity): Boolean = oldItem == newItem
    }
}