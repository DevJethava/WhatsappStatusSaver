package com.example.whatsappstatussaver

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.GridLayoutManager
import com.example.whatsappstatussaver.adapter.StatusAdapter
import com.example.whatsappstatussaver.databinding.ActivityMainBinding
import com.example.whatsappstatussaver.model.FileModel
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private var listStatus: MutableList<FileModel> = mutableListOf()
    private lateinit var adapter: StatusAdapter

    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar!!.title = "All Status"

        adapter = StatusAdapter(this, listStatus, fun(model: FileModel, pos: Int) {
            saveFile(model)
        })
        binding.rvStatusList.layoutManager = GridLayoutManager(this, 2)
        binding.rvStatusList.adapter = adapter

        val result = readDataFromPrefs()

        if (result) {
            listStatus.clear()
            val sh = getSharedPreferences("DATA_PATH", MODE_PRIVATE)
            val uriPath = sh.getString("PATH", "")

            contentResolver.takePersistableUriPermission(
                Uri.parse(uriPath),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            if (!uriPath.isNullOrEmpty()) {
                val fileDoc = DocumentFile.fromTreeUri(applicationContext, Uri.parse(uriPath))
                for (file: DocumentFile in fileDoc!!.listFiles()) {
                    if (!file.name!!.endsWith(".nomedia")) {
                        listStatus.add(FileModel(file.name!!, file.uri.toString()))
                    }
                }

                if (this::adapter.isInitialized) {
                    adapter.addList(listStatus)
                }
            }
        } else {
            getFolderPermission()
        }

    }

    private fun saveFile(model: FileModel) {
        if (model.fileUri.endsWith(".mp4")) {
            val inputStream = contentResolver.openInputStream(Uri.parse(model.fileUri))
            val fileName = "${System.currentTimeMillis()}.mp4"
            try {
                val value = ContentValues()
                value.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                value.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                value.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOCUMENTS + "/Videos/"
                )
                val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), value)
                val outputStream: OutputStream = uri?.let {
                    contentResolver.openOutputStream(it)
                }!!
                if (inputStream != null) {
                    outputStream.write(inputStream.readBytes())
                }
                outputStream.close()
                Toast.makeText(this, "Video Saved successfully!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Fail to save Video.", Toast.LENGTH_LONG).show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getFolderPermission() {
        val storageManager = application.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val intent = storageManager.primaryStorageVolume.createOpenDocumentTreeIntent()
        val targetDirectory = "Android%2Fmedia%2Fcom.whatsapp%2FWhatsApp%2FMedia%2F.Statuses"
        var uri = intent.getParcelableExtra<Uri>("android.provider.extra.INITIAL_URI") as Uri
        var schema = uri.toString()
        schema = schema.replace("/root/", "/tree/")
        schema += "%3A${targetDirectory}"
        uri = Uri.parse(schema)
        intent.putExtra("android.provider.extra.INITIAL_URI", uri)
        intent.putExtra("android.content.extra.SHOW_ADVANCE", true)
        startActivityForResult(intent, 1234)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            data?.data?.let {

                val sharedPreferences = getSharedPreferences("DATA_PATH", MODE_PRIVATE)
                val myEdit = sharedPreferences.edit()
                myEdit.putString("PATH", it.toString())
                myEdit.apply()

                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val fileDoc = DocumentFile.fromTreeUri(applicationContext, it)
                for (file: DocumentFile in fileDoc!!.listFiles()) {
                    if (!file.name!!.endsWith(".nomedia")) {
                        listStatus.add(FileModel(file.name!!, file.uri.toString()))
                    }
                }

                if (this::adapter.isInitialized) {
                    adapter.addList(listStatus)
                }
            }
        }
    }

    private fun readDataFromPrefs(): Boolean {
        val sh = getSharedPreferences("DATA_PATH", MODE_PRIVATE)
        val uriPath = sh.getString("PATH", "")
        if (!uriPath.isNullOrEmpty())
            return true
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}