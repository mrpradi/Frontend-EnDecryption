package com.simats.endecryption

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.simats.endecryption.network.ApiClient
import com.simats.endecryption.network.EncryptionResponse
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

    private lateinit var uploadFileCard: MaterialCardView
    private lateinit var uploadedFileInfoCard: MaterialCardView
    private lateinit var encryptButton: Button
    private lateinit var encryptingStatus: LinearLayout
    private lateinit var completeActions: LinearLayout
    private lateinit var downloadEncryptedFileButton: Button
    private lateinit var viewInSavedFilesButton: Button
    private lateinit var fileNameText: TextView
    private lateinit var fileSizeText: TextView
    private lateinit var errorMessageText: TextView
    
    private lateinit var encryptionStepsContainer: LinearLayout
    private lateinit var step1Card: MaterialCardView
    private lateinit var step2Card: MaterialCardView
    private lateinit var step3Card: MaterialCardView
    private lateinit var step1Icon: ImageView
    private lateinit var step2Icon: ImageView
    private lateinit var step3Icon: ImageView
    
    private lateinit var decryptionKeyCard: MaterialCardView
    private lateinit var decryptionKeyText: TextView
    private lateinit var copyKeyButton: Button

    private var fileUri: Uri? = null
    private var userEmail: String? = null
    private var encryptedData: EncryptionResponse? = null

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

        initViews()

        uploadFileCard.setOnClickListener { openFilePicker() }

        encryptButton.setOnClickListener {
            // Mimic React check for consent
            val settingsPref = getSharedPreferences("PrivacySettings", Context.MODE_PRIVATE)
            val hasConsent = settingsPref.getBoolean("dataUsageConsent", true) // Default true for this app logic
            
            if (!hasConsent) {
                showConsentDialog()
                return@setOnClickListener
            }

            fileUri?.let { uri ->
                if (userEmail != null) uploadAndEncryptFile(userEmail!!, uri)
                else Toast.makeText(this, "Email not found. Please login again.", Toast.LENGTH_SHORT).show()
            }
        }

        copyKeyButton.setOnClickListener {
            val key = decryptionKeyText.text.toString()
            if (key.isNotEmpty()) {
                copyToClipboard(key)
            }
        }

        downloadEncryptedFileButton.setOnClickListener {
            encryptedData?.let { data ->
                downloadEncryptedFile(data.fileId, data.encryptedFileName)
            }
        }

        viewInSavedFilesButton.setOnClickListener {
            val intent = Intent(this, SavedFilesActivity::class.java)
            intent.putExtra("EMAIL", userEmail)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.back_button).setOnClickListener { finish() }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        setupBottomNavigation(bottomNavigationView, R.id.navigation_home)
    }

    private fun initViews() {
        uploadFileCard = findViewById(R.id.upload_file_card)
        uploadedFileInfoCard = findViewById(R.id.uploaded_file_info_card)
        encryptButton = findViewById(R.id.encrypt_button)
        encryptingStatus = findViewById(R.id.encrypting_status)
        completeActions = findViewById(R.id.complete_actions)
        downloadEncryptedFileButton = findViewById(R.id.download_encrypted_file_button)
        viewInSavedFilesButton = findViewById(R.id.view_in_saved_files_button)
        fileNameText = findViewById(R.id.file_name_text)
        fileSizeText = findViewById(R.id.file_size_text)
        errorMessageText = findViewById(R.id.error_message_text)
        
        encryptionStepsContainer = findViewById(R.id.encryption_steps_container)
        step1Card = findViewById(R.id.step1_card)
        step2Card = findViewById(R.id.step2_card)
        step3Card = findViewById(R.id.step3_card)
        step1Icon = findViewById(R.id.step1_icon)
        step2Icon = findViewById(R.id.step2_icon)
        step3Icon = findViewById(R.id.step3_icon)
        
        decryptionKeyCard = findViewById(R.id.decryption_key_card)
        decryptionKeyText = findViewById(R.id.decryption_key_text)
        copyKeyButton = findViewById(R.id.copy_key_button)
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Decryption Key", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Decryption key copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun showConsentDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Data Usage Consent Required")
            .setMessage("You must provide consent for data usage in Privacy Settings to encrypt files.")
            .setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(this, PrivacySecurityActivity::class.java))
            }
            .setNegativeButton("Cancel", null)
            .show()
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

                uploadedFileInfoCard.visibility = View.VISIBLE
                encryptButton.visibility = View.VISIBLE
                encryptButton.isEnabled = true
                
                // Reset states
                encryptionStepsContainer.visibility = View.GONE
                decryptionKeyCard.visibility = View.GONE
                completeActions.visibility = View.GONE
                encryptingStatus.visibility = View.GONE
                errorMessageText.visibility = View.GONE
                
                resetStepUI()
            }
        }
    }

    private fun resetStepUI() {
        step1Card.strokeWidth = 0
        step2Card.strokeWidth = 0
        step3Card.strokeWidth = 0
        
        findViewById<LinearLayout>(R.id.step1_layout).setBackgroundColor(android.graphics.Color.TRANSPARENT)
        findViewById<LinearLayout>(R.id.step2_layout).setBackgroundColor(android.graphics.Color.TRANSPARENT)
        findViewById<LinearLayout>(R.id.step3_layout).setBackgroundColor(android.graphics.Color.TRANSPARENT)

        step1Icon.setImageResource(R.drawable.ic_lock)
        step2Icon.setImageResource(R.drawable.ic_key)
        step3Icon.setImageResource(R.drawable.ic_shield)
        
        step1Icon.imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.teal_400))
        step2Icon.imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_dim))
        step3Icon.imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_dim))
    }

    private fun setStepUI(step: Int) {
        encryptionStepsContainer.visibility = View.VISIBLE
        when (step) {
            1 -> {
                findViewById<LinearLayout>(R.id.step1_layout).setBackgroundColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(this, R.color.teal_400), 51))
                step1Card.strokeWidth = 2
                step1Card.strokeColor = ContextCompat.getColor(this, R.color.teal_400)
            }
            2 -> {
                step1Icon.setImageResource(R.drawable.ic_check_circle)
                step1Icon.imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_400))
                findViewById<LinearLayout>(R.id.step1_layout).setBackgroundColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(this, R.color.green_400), 51))
                step1Card.strokeColor = ContextCompat.getColor(this, R.color.green_400)

                findViewById<LinearLayout>(R.id.step2_layout).setBackgroundColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(this, R.color.teal_400), 51))
                step2Card.strokeWidth = 2
                step2Card.strokeColor = ContextCompat.getColor(this, R.color.teal_400)
                step2Icon.imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.teal_400))
            }
            3 -> {
                step2Icon.setImageResource(R.drawable.ic_check_circle)
                step2Icon.imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_400))
                findViewById<LinearLayout>(R.id.step2_layout).setBackgroundColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(this, R.color.green_400), 51))
                step2Card.strokeColor = ContextCompat.getColor(this, R.color.green_400)

                findViewById<LinearLayout>(R.id.step3_layout).setBackgroundColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(this, R.color.teal_400), 51))
                step3Card.strokeWidth = 2
                step3Card.strokeColor = ContextCompat.getColor(this, R.color.teal_400)
                step3Icon.imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.teal_400))
            }
            4 -> { 
                step3Icon.setImageResource(R.drawable.ic_check_circle)
                step3Icon.imageTintList = android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_400))
                findViewById<LinearLayout>(R.id.step3_layout).setBackgroundColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(this, R.color.green_400), 51))
                step3Card.strokeColor = ContextCompat.getColor(this, R.color.green_400)
            }
        }
    }

    private fun uploadAndEncryptFile(email: String, uri: Uri) {
        val file = getFileFromUri(uri) ?: return
        val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val emailPart = email.toRequestBody("text/plain".toMediaTypeOrNull())

        encryptButton.visibility = View.GONE
        encryptingStatus.visibility = View.VISIBLE
        errorMessageText.visibility = View.GONE
        
        setStepUI(1)

        ApiClient.instance.encryptFile(emailPart, body).enqueue(object : Callback<EncryptionResponse> {
            override fun onResponse(call: Call<EncryptionResponse>, response: Response<EncryptionResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    setStepUI(2)
                    encryptedData = response.body()
                    
                    encryptionStepsContainer.postDelayed({
                        setStepUI(3)
                        encryptionStepsContainer.postDelayed({
                            setStepUI(4)
                            finalizeEncryption(file.name)
                        }, 800)
                    }, 800)

                } else {
                    encryptingStatus.visibility = View.GONE
                    encryptButton.visibility = View.VISIBLE
                    val error = response.errorBody()?.string() ?: "Encryption failed"
                    errorMessageText.text = error
                    errorMessageText.visibility = View.VISIBLE
                    encryptionStepsContainer.visibility = View.GONE
                }
            }
            override fun onFailure(call: Call<EncryptionResponse>, t: Throwable) {
                encryptingStatus.visibility = View.GONE
                encryptButton.visibility = View.VISIBLE
                errorMessageText.text = "Network Error: ${t.message}"
                errorMessageText.visibility = View.VISIBLE
                encryptionStepsContainer.visibility = View.GONE
            }
        })
    }

    private fun finalizeEncryption(originalFileName: String) {
        encryptingStatus.visibility = View.GONE
        decryptionKeyText.text = encryptedData?.decryptionKey
        decryptionKeyCard.visibility = View.VISIBLE
        completeActions.visibility = View.VISIBLE
        
        val jsonContent = com.google.gson.Gson().toJson(encryptedData)
        saveEncryptedFileToAppDir(jsonContent, encryptedData!!.encryptedFileName)
        
        NotificationHelper.showNotification(
            this@EncryptionActivity,
            "File Encrypted Successfully",
            "Your file '$originalFileName' was encrypted using Hybrid AES+RSA protection.",
            R.drawable.ic_shield,
            "security_alerts"
        )
    }

    private fun downloadEncryptedFile(fileId: Int, fileName: String) {
        downloadEncryptedFileButton.isEnabled = false
        ApiClient.instance.downloadEncryptedFile(fileId).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                downloadEncryptedFileButton.isEnabled = true
                if (response.isSuccessful && response.body() != null) {
                    val bytes = response.body()!!.bytes()
                    if (saveBytesToPublicStorage(bytes, fileName)) {
                        Toast.makeText(this@EncryptionActivity, "Downloaded to Downloads/EnDecryption", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@EncryptionActivity, "Download failed", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@EncryptionActivity, "Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                downloadEncryptedFileButton.isEnabled = true
                Toast.makeText(this@EncryptionActivity, "Download Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveEncryptedFileToAppDir(content: String, fileName: String) {
        try {
            val userDirName = userEmail?.replace("@", "_")?.replace(".", "_") ?: "anonymous"
            val dir = File(filesDir, "encrypted/$userDirName")
            if (!dir.exists()) dir.mkdirs()
            FileOutputStream(File(dir, fileName)).use { it.write(content.toByteArray()) }
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
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
            Log.e("Encryption", "Download Error", e)
            false 
        }
    }
}
