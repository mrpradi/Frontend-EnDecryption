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
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
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
import java.net.URLConnection

class DecryptionActivity : BaseActivity() {

    private lateinit var uploadFileCard: CardView
    private lateinit var uploadedFileInfoCard: CardView
    private lateinit var decryptButton: Button
    private lateinit var downloadDecryptedFileButton: Button
    private lateinit var fileNameText: TextView
    private lateinit var fileSizeText: TextView
    private lateinit var decryptionKeyInput: EditText

    private var fileUri: Uri? = null
    private var userEmail: String? = null
    private var decryptedBytes: ByteArray? = null
    private var serverFileName: String? = null

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
        decryptionKeyInput = findViewById(R.id.decryption_key_input)

        uploadFileCard.setOnClickListener { openFilePicker() }

        decryptButton.setOnClickListener {
            val key = decryptionKeyInput.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(this, "Please enter decryption key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            fileUri?.let { decryptUploadedFile(it, key) } ?: Toast.makeText(this, "Select a file", Toast.LENGTH_SHORT).show()
        }

        decryptionKeyInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                decryptButton.isEnabled = s.toString().trim().isNotEmpty() && fileUri != null
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        downloadDecryptedFileButton.setOnClickListener {
            decryptedBytes?.let { bytes ->
                // Prioritize filename from server response headers if available
                var originalName = serverFileName ?: fileNameText.text.toString()
                
                // Enhanced name cleaning
                originalName = originalName.removePrefix("dec_temp_") // Match backend prefix
                    .removePrefix("decrypted_")
                    .removePrefix("secure_")
                    .removeSuffix(".json")
                    .removeSuffix(".encrypted")
                    .removeSuffix(".enc")
                
                // If it's still just a generic name, add prefix to identify as decrypted
                if (!originalName.contains(".") && !originalName.equals("to_decrypt", true)) {
                    originalName = "decrypted_" + originalName
                }

                if (saveBytesToPublicStorage(bytes, originalName)) {
                    Toast.makeText(this, "File '$originalName' saved to Downloads/EnDecryption", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<View>(R.id.back_button).setOnClickListener { finish() }
        
        // Check for incoming file from SavedFiles
        val incomingFileId = intent.getIntExtra("FILE_ID", -1)
        val incomingFileName = intent.getStringExtra("FILE_NAME")
        if (incomingFileId != -1) {
            handleIncomingSavedFile(incomingFileId, incomingFileName ?: "file.encrypted")
        }
    }

    private fun handleIncomingSavedFile(fileId: Int, name: String) {
        fileNameText.text = name
        fileSizeText.text = "Fetching from safe..."
        uploadFileCard.visibility = View.GONE
        uploadedFileInfoCard.visibility = View.VISIBLE
        decryptButton.visibility = View.VISIBLE
        decryptButton.isEnabled = false

        ApiClient.instance.downloadEncryptedFile(fileId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful && response.body() != null) {
                    try {
                        val tempFile = File(cacheDir, name)
                        response.body()!!.byteStream().use { input ->
                            tempFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        fileUri = Uri.fromFile(tempFile)
                        fileSizeText.text = String.format("%.2f KB", tempFile.length() / 1024.0)
                        decryptButton.isEnabled = decryptionKeyInput.text.toString().trim().isNotEmpty()
                        Toast.makeText(this@DecryptionActivity, "File ready for decryption", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        onError("Failed to prepare file: ${e.message}")
                    }
                } else {
                    onError("Failed to download file from server")
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                onError("Network error: ${t.message}")
            }
            private fun onError(message: String) {
                Toast.makeText(this@DecryptionActivity, message, Toast.LENGTH_LONG).show()
                uploadFileCard.visibility = View.VISIBLE
                uploadedFileInfoCard.visibility = View.GONE
            }
        })
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
                if (sizeIndex != -1) fileSizeText.text = String.format("%.2f KB", it.getLong(sizeIndex) / 1024.0)
                
                uploadFileCard.visibility = View.GONE
                uploadedFileInfoCard.visibility = View.VISIBLE
                decryptButton.visibility = View.VISIBLE
                decryptButton.isEnabled = decryptionKeyInput.text.toString().trim().isNotEmpty()
            }
        }
    }

    private fun decryptUploadedFile(uri: Uri, key: String) {
        val file = getFileFromUri(uri) ?: return
        val body = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody("multipart/form-data".toMediaTypeOrNull()))
        val keyBody = okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), key)
        val emailBody = userEmail?.let { okhttp3.RequestBody.create("text/plain".toMediaTypeOrNull(), it) }

        decryptButton.isEnabled = false
        decryptButton.text = "Decrypting..."

        ApiClient.instance.decryptFile(body, keyBody, emailBody).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                decryptButton.isEnabled = true
                decryptButton.text = "Decrypt File"
                if (response.isSuccessful && response.body() != null) {
                    decryptedBytes = response.body()!!.bytes()
                    
                    // Extract filename from Content-Disposition header (if returned by server)
                    val disposition = response.headers()["Content-Disposition"]
                    if (disposition != null && disposition.contains("filename=")) {
                        serverFileName = disposition.split("filename=")[1]
                            .trim()
                            .replace("\"", "")
                            .split(";")[0]
                    }

                    downloadDecryptedFileButton.visibility = View.VISIBLE
                    Toast.makeText(this@DecryptionActivity, "Decryption successful", Toast.LENGTH_SHORT).show()

                    NotificationHelper.showNotification(
                        this@DecryptionActivity,
                        "Decryption Successful",
                        "File decrypted successfully. Tap download to save.",
                        R.drawable.ic_shield,
                        "decryption_alerts"
                    )
                } else {
                    val error = response.errorBody()?.string() ?: "Invalid decryption key or corrupted file"
                    Toast.makeText(this@DecryptionActivity, error, Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                decryptButton.isEnabled = true
                decryptButton.text = "Decrypt File"
                Toast.makeText(this@DecryptionActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val name = fileNameText.text.toString().ifEmpty { "to_decrypt" }
            val file = File(cacheDir, name)
            contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            file
        } catch (e: Exception) { null }
    }

    private fun saveBytesToPublicStorage(bytes: ByteArray, fileName: String): Boolean {
        return try {
            val mimeType = URLConnection.guessContentTypeFromName(fileName) ?: "application/octet-stream"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/EnDecryption")
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let { contentResolver.openOutputStream(it)?.use { os -> os.write(bytes) } }
                true
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val appDir = File(downloadsDir, "EnDecryption")
                if (!appDir.exists()) appDir.mkdirs()
                val file = File(appDir, fileName)
                FileOutputStream(file).use { it.write(bytes) }
                true
            }
        } catch (e: Exception) { 
            Log.e("Decryption", "Download Error", e)
            false 
        }
    }
}
