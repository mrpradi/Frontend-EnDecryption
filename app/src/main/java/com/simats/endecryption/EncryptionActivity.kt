package com.simats.endecryption

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.simats.endecryption.network.ApiClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class EncryptionActivity : BaseActivity() {

    private lateinit var uploadFileCard: CardView
    private lateinit var uploadedFileInfoCard: CardView
    private lateinit var encryptButton: Button
    private lateinit var downloadEncryptedFileButton: Button
    private lateinit var viewInSavedFilesButton: Button
    private lateinit var fileNameText: TextView
    private lateinit var fileSizeText: TextView

    private var fileUri: Uri? = null
    private var userEmail: String? = null
    private var encryptedBytes: ByteArray? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                fileUri = uri
                displayFileInfo(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encryption)

        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userEmail = intent.getStringExtra("EMAIL") ?: sharedPref.getString("EMAIL", null)

        uploadFileCard = findViewById(R.id.upload_file_card)
        uploadedFileInfoCard = findViewById(R.id.uploaded_file_info_card)
        encryptButton = findViewById(R.id.encrypt_button)
        downloadEncryptedFileButton = findViewById(R.id.download_encrypted_file_button)
        viewInSavedFilesButton = findViewById(R.id.view_in_saved_files_button)
        fileNameText = findViewById(R.id.file_name_text)
        fileSizeText = findViewById(R.id.file_size_text)

        uploadFileCard.setOnClickListener { openFilePicker() }

        encryptButton.setOnClickListener {
            fileUri?.let { uri ->
                if (userEmail != null) uploadAndEncryptFile(userEmail!!, uri)
                else Toast.makeText(this, "Email not found. Please login again.", Toast.LENGTH_SHORT).show()
            }
        }

        downloadEncryptedFileButton.setOnClickListener {
            encryptedBytes?.let { bytes ->
                val originalName = fileNameText.text.toString()
                val encryptedName = "secure_$originalName.json"
                if (saveBytesToPublicStorage(bytes, encryptedName)) {
                    Toast.makeText(this, "Encrypted JSON saved to Downloads/EnDecryption", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to save encrypted file", Toast.LENGTH_SHORT).show()
                }
            }
        }

        viewInSavedFilesButton.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            intent.putExtra("EMAIL", userEmail)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.back_button).setOnClickListener { finish() }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        setupBottomNavigation(bottomNavigationView, 0)
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "*/*" }
        filePickerLauncher.launch(intent)
    }

    private fun displayFileInfo(uri: Uri) {
        contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (nameIndex != -1) fileNameText.text = it.getString(nameIndex)
                if (sizeIndex != -1) fileSizeText.text = "${it.getLong(sizeIndex) / 1024} KB"

                uploadFileCard.visibility = View.GONE
                uploadedFileInfoCard.visibility = View.VISIBLE
                encryptButton.visibility = View.VISIBLE
                encryptButton.isEnabled = true
                encryptedBytes = null
            }
        }
    }

    private fun uploadAndEncryptFile(email: String, uri: Uri) {
        val file = getFileFromUri(uri) ?: return
        val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val emailPart = email.toRequestBody("text/plain".toMediaTypeOrNull())

        encryptButton.isEnabled = false
        ApiClient.instance.encryptFile(emailPart, body).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                encryptButton.isEnabled = true
                if (response.isSuccessful && response.body() != null) {
                    encryptedBytes = response.body()!!.bytes()
                    downloadEncryptedFileButton.visibility = View.VISIBLE
                    viewInSavedFilesButton.visibility = View.VISIBLE
                    
                    saveEncryptedFileToAppDir(encryptedBytes!!, "secure_" + file.name + ".json")
                    Toast.makeText(this@EncryptionActivity, "File encrypted successfully", Toast.LENGTH_SHORT).show()

                    // Trigger Push Notification
                    NotificationHelper.showNotification(
                        this@EncryptionActivity,
                        "Encryption Successful",
                        "File '${file.name}' has been successfully encrypted.",
                        R.drawable.ic_shield,
                        "encryption_completion"
                    )
                } else {
                    val error = response.errorBody()?.string() ?: "Encryption failed"
                    Toast.makeText(this@EncryptionActivity, error, Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                encryptButton.isEnabled = true
                Toast.makeText(this@EncryptionActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveEncryptedFileToAppDir(bytes: ByteArray, fileName: String) {
        try {
            val userDirName = userEmail?.replace("@", "_")?.replace(".", "_") ?: "anonymous"
            val dir = File(filesDir, "encrypted/$userDirName")
            if (!dir.exists()) dir.mkdirs()
            FileOutputStream(File(dir, fileName)).use { it.write(bytes) }
        } catch (e: Exception) { Log.e("Encryption", "Error", e) }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val file = File(cacheDir, getFileName(uri) ?: "temp")
            FileOutputStream(file).use { inputStream.copyTo(it) }
            file
        } catch (e: Exception) { null }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) name = it.getString(index)
            }
        }
        return name
    }

    private fun saveBytesToPublicStorage(bytes: ByteArray, fileName: String): Boolean {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EnDecryption")
                }
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { contentResolver.openOutputStream(it)?.use { os -> os.write(bytes) } }
            true
        } catch (e: Exception) { false }
    }
}
