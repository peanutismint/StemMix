package com.stemmix

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.io.File

class ModelManagerActivity : AppCompatActivity() {
    
    private lateinit var modelDownloader: ModelDownloader
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ModelAdapter
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Handle imported file
                val contentResolver = contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                
                inputStream?.use { input ->
                    val tempFile = File.createTempFile("imported_model", ".tflite", cacheDir)
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    
                    // Show dialog to select which model this should be
                    showModelSelectionDialog(tempFile)
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_manager)
        
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Model Manager"
        
        modelDownloader = ModelDownloader(this)
        
        recyclerView = findViewById(R.id.modelRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        adapter = ModelAdapter(
            models = ModelDownloader.AVAILABLE_MODELS,
            modelDownloader = modelDownloader,
            onDownload = { model -> downloadModel(model) },
            onDelete = { model -> deleteModel(model) },
            onSelect = { model -> selectModel(model) }
        )
        
        recyclerView.adapter = adapter
        
        findViewById<Button>(R.id.btnImportModel).setOnClickListener {
            importModelFromFile()
        }
        
        updateStorageInfo()
    }
    
    private fun downloadModel(model: ModelDownloader.ModelInfo) {
        AlertDialog.Builder(this)
            .setTitle("Download ${model.displayName}")
            .setMessage("This will download ${formatFileSize(model.fileSize)}. Continue?")
            .setPositiveButton("Download") { _, _ ->
                startDownload(model)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun startDownload(model: ModelDownloader.ModelInfo) {
        val position = ModelDownloader.AVAILABLE_MODELS.indexOf(model)
        
        lifecycleScope.launch {
            val result = modelDownloader.downloadModel(model) { downloaded, total, percentage ->
                adapter.updateProgress(position, percentage)
            }
            
            result.onSuccess {
                adapter.updateDownloadStatus(position, true)
                Toast.makeText(
                    this@ModelManagerActivity,
                    "${model.displayName} downloaded successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                updateStorageInfo()
            }
            
            result.onFailure { error ->
                adapter.updateProgress(position, 0)
                AlertDialog.Builder(this@ModelManagerActivity)
                    .setTitle("Download Failed")
                    .setMessage("Failed to download ${model.displayName}: ${error.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private fun deleteModel(model: ModelDownloader.ModelInfo) {
        AlertDialog.Builder(this)
            .setTitle("Delete ${model.displayName}")
            .setMessage("This will free up ${formatFileSize(modelDownloader.getModelSize(model.id))}. Continue?")
            .setPositiveButton("Delete") { _, _ ->
                if (modelDownloader.deleteModel(model.id)) {
                    val position = ModelDownloader.AVAILABLE_MODELS.indexOf(model)
                    adapter.updateDownloadStatus(position, false)
                    Toast.makeText(this, "Model deleted", Toast.LENGTH_SHORT).show()
                    updateStorageInfo()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun selectModel(model: ModelDownloader.ModelInfo) {
        // Save selected model to preferences
        val prefs = getSharedPreferences("StemMixPrefs", MODE_PRIVATE)
        prefs.edit().putString("selected_model", model.id).apply()
        
        Toast.makeText(this, "${model.displayName} selected", Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun importModelFromFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        filePickerLauncher.launch(intent)
    }
    
    private fun showModelSelectionDialog(sourceFile: File) {
        val modelNames = ModelDownloader.AVAILABLE_MODELS.map { it.displayName }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Import as which model?")
            .setItems(modelNames) { _, which ->
                val model = ModelDownloader.AVAILABLE_MODELS[which]
                lifecycleScope.launch {
                    val result = modelDownloader.importModel(model.id, sourceFile)
                    
                    result.onSuccess {
                        adapter.updateDownloadStatus(which, true)
                        Toast.makeText(
                            this@ModelManagerActivity,
                            "Model imported successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                        updateStorageInfo()
                    }
                    
                    result.onFailure { error ->
                        Toast.makeText(
                            this@ModelManagerActivity,
                            "Import failed: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    
                    sourceFile.delete()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                sourceFile.delete()
            }
            .show()
    }
    
    private fun updateStorageInfo() {
        val totalSize = modelDownloader.getTotalModelsSize()
        val downloadedCount = modelDownloader.getDownloadedModels().size
        
        findViewById<TextView>(R.id.storageInfo).text = 
            "Downloaded: $downloadedCount models (${formatFileSize(totalSize)})"
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "${bytes / 1_073_741_824}GB"
            bytes >= 1_048_576 -> "${bytes / 1_048_576}MB"
            bytes >= 1024 -> "${bytes / 1024}KB"
            else -> "${bytes}B"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class ModelAdapter(
    private val models: List<ModelDownloader.ModelInfo>,
    private val modelDownloader: ModelDownloader,
    private val onDownload: (ModelDownloader.ModelInfo) -> Unit,
    private val onDelete: (ModelDownloader.ModelInfo) -> Unit,
    private val onSelect: (ModelDownloader.ModelInfo) -> Unit
) : RecyclerView.Adapter<ModelAdapter.ModelViewHolder>() {
    
    private val downloadProgress = MutableList(models.size) { 0 }
    private val downloadStatus = MutableList(models.size) { false }
    
    init {
        // Initialize download status
        models.forEachIndexed { index, model ->
            downloadStatus[index] = modelDownloader.isModelDownloaded(model.id)
        }
    }
    
    class ModelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.modelName)
        val descText: TextView = view.findViewById(R.id.modelDescription)
        val statusText: TextView = view.findViewById(R.id.modelStatus)
        val progressBar: ProgressBar = view.findViewById(R.id.downloadProgress)
        val downloadBtn: Button = view.findViewById(R.id.btnDownload)
        val deleteBtn: Button = view.findViewById(R.id.btnDelete)
        val selectBtn: Button = view.findViewById(R.id.btnSelect)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_model, parent, false)
        return ModelViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        val model = models[position]
        val isDownloaded = downloadStatus[position]
        val progress = downloadProgress[position]
        
        holder.nameText.text = model.displayName
        holder.descText.text = model.description
        
        when {
            progress > 0 && progress < 100 -> {
                holder.statusText.text = "Downloading... $progress%"
                holder.progressBar.visibility = View.VISIBLE
                holder.progressBar.progress = progress
                holder.downloadBtn.visibility = View.GONE
                holder.deleteBtn.visibility = View.GONE
                holder.selectBtn.visibility = View.GONE
            }
            isDownloaded -> {
                val size = modelDownloader.getModelSize(model.id)
                holder.statusText.text = "Downloaded (${formatFileSize(size)})"
                holder.progressBar.visibility = View.GONE
                holder.downloadBtn.visibility = View.GONE
                holder.deleteBtn.visibility = View.VISIBLE
                holder.selectBtn.visibility = View.VISIBLE
            }
            else -> {
                holder.statusText.text = "Not downloaded (${formatFileSize(model.fileSize)})"
                holder.progressBar.visibility = View.GONE
                holder.downloadBtn.visibility = View.VISIBLE
                holder.deleteBtn.visibility = View.GONE
                holder.selectBtn.visibility = View.GONE
            }
        }
        
        holder.downloadBtn.setOnClickListener { onDownload(model) }
        holder.deleteBtn.setOnClickListener { onDelete(model) }
        holder.selectBtn.setOnClickListener { onSelect(model) }
    }
    
    override fun getItemCount() = models.size
    
    fun updateProgress(position: Int, progress: Int) {
        downloadProgress[position] = progress
        notifyItemChanged(position)
    }
    
    fun updateDownloadStatus(position: Int, isDownloaded: Boolean) {
        downloadStatus[position] = isDownloaded
        downloadProgress[position] = 0
        notifyItemChanged(position)
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> "${bytes / 1_073_741_824}GB"
            bytes >= 1_048_576 -> "${bytes / 1_048_576}MB"
            bytes >= 1024 -> "${bytes / 1024}KB"
            else -> "${bytes}B"
        }
    }
}
