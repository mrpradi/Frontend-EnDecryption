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
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream

class DecryptionActivity : BaseActivity() {

    private lateinit var uploadFileCard: CardView
    private lateinit var uploadedFileInfoCard: CardView
    private lateinit var decryptButton: Button
    private lateinit var downloadDecryptedFileButton: Button
    private lateinit var fileNameText: TextView
    private lateinit var fileSizeText: TextView

    private var fileUri: Uri? = null
    private var userEmail: String? = null
    private var decryptedBytes: ByteArray? = null

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
        setContentView(R.layout.activity_decryption)

        val sharedPref = getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        userEmail = intent.getStringExtra("EMAIL") ?: sharedPref.getString("EMAIL", null)

        uploadFileCard = findViewById(R.id.upload_file_card)
        uploadedFileInfoCard = findViewById(R.id.uploaded_file_info_card)
        decryptButton = findViewById(R.id.decrypt_button)
        downloadDecryptedFileButton = findViewById(R.id.download_decrypted_file_button)
        fileNameText = findViewById(R.id.file_name_text)
        fileSizeText = findViewById(R.id.file_size_text)

        uploadFileCard.setOnClickListener { openFilePicker() }

        decryptButton.setOnClickListener {
            fileUri?.let { decryptUploadedFile(it) } ?: Toast.makeText(this, "Select a file", Toast.LENGTH_SHORT).show()
        }

        downloadDecryptedFileButton.setOnClickListener {
            decryptedBytes?.let { bytes ->
                if (saveBytesToPublicStorage(bytes, "decrypted_file")) {
                    Toast.makeText(this, "Decrypted file saved to Downloads", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<View>(R.id.back_button).setOnClickListener { finish() }
        setupBottomNavigation(findViewById(R.id.bottom_navigation), 0)
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
                fileNameText.text = it.getString(nameIndex)
                fileSizeText.text = "${it.getLong(sizeIndex) / 1024} KB"
                uploadFileCard.visibility = View.GONE
                uploadedFileInfoCard.visibility = View.VISIBLE
                decryptButton.visibility = View.VISIBLE
            }
        }
    }

    private fun decryptUploadedFile(uri: Uri) {
        val file = getFileFromUri(uri) ?: return
        val body = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody("multipart/form-data".toMediaTypeOrNull()))

        decryptButton.isEnabled = false
        ApiClient.instance.decryptFile(body).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                decryptButton.isEnabled = true
                if (response.isSuccessful && response.body() != null) {
                    decryptedBytes = response.body()!!.bytes()
                    downloadDecryptedFileButton.visibility = View.VISIBLE
                    Toast.makeText(this@DecryptionActivity, "Decryption successful", Toast.LENGTH_SHORT).show()

                    // Trigger Push Notification
                    NotificationHelper.showNotification(
                        this@DecryptionActivity,
                        "Decryption Successful",
                        "File '${file.name}' has been successfully decrypted.",
                        R.drawable.ic_shield,
                        "decryption_alerts"
                    )
                } else {
                    Toast.makeText(this@DecryptionActivity, "Decryption failed", Toast.LENGTH_LONG).show()

                    // Trigger Failure Notification
                    NotificationHelper.showNotification(
                        this@DecryptionActivity,
                        "Decryption Failed",
                        "Failed to decrypt file '${file.name}'.",
                        R.drawable.ic_warning,
                        "decryption_alerts"
                    )
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                decryptButton.isEnabled = true
                Toast.makeText(this@DecryptionActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val file = File(cacheDir, "to_decrypt")
            FileOutputStream(file).use { os -> contentResolver.openInputStream(uri)?.copyTo(os) }
            file
        } catch (e: Exception) { null }
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
