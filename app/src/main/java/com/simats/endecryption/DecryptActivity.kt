package com.simats.endecryption

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.io.FileOutputStream
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class DecryptActivity : AppCompatActivity() {

    private lateinit var uploadEncryptedFileCard: CardView
    private lateinit var uploadedFileInfoCard: CardView
    private lateinit var decryptionKeyInput: EditText
    private lateinit var decryptButton: Button
    private lateinit var decryptionStepsLayout: LinearLayout
    private lateinit var downloadDecryptedFileButton: Button
    private lateinit var fileNameText: TextView
    private lateinit var fileSizeText: TextView

    private var encryptedFileUri: Uri? = null
    private var decryptedData: ByteArray? = null

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            it.data?.data?.let { uri ->
                encryptedFileUri = uri
                displayFileInfo(uri)
            }
        }
    }

    private val saveDecryptedFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        uri?.let {
            saveFile(it, decryptedData)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decrypt)

        uploadEncryptedFileCard = findViewById(R.id.upload_encrypted_file_card)
        uploadedFileInfoCard = findViewById(R.id.uploaded_file_info_card)
        decryptionKeyInput = findViewById(R.id.decryption_key_input)
        decryptButton = findViewById(R.id.decrypt_button)
        decryptionStepsLayout = findViewById(R.id.decryption_steps_layout)
        downloadDecryptedFileButton = findViewById(R.id.download_decrypted_file_button)
        fileNameText = findViewById(R.id.file_name_text)
        fileSizeText = findViewById(R.id.file_size_text)

        uploadEncryptedFileCard.setOnClickListener {
            openFilePicker()
        }

        decryptionKeyInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                decryptButton.isEnabled = encryptedFileUri != null && s?.isNotEmpty() == true
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        decryptButton.setOnClickListener {
            encryptedFileUri?.let {
                decryptFile(it, decryptionKeyInput.text.toString())
            }
        }

        downloadDecryptedFileButton.setOnClickListener {
            decryptedData?.let {
                val originalFileName = fileNameText.text.toString().replace(".encrypted", "")
                saveDecryptedFileLauncher.launch(originalFileName)
            }
        }

        findViewById<TextView>(R.id.back_button).setOnClickListener {
            finish()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        filePickerLauncher.launch(intent)
    }

    private fun displayFileInfo(uri: Uri) {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                val name = it.getString(nameIndex)
                val size = it.getLong(sizeIndex)

                fileNameText.text = name
                fileSizeText.text = "${size / 1024} KB"

                uploadedFileInfoCard.visibility = View.VISIBLE
                decryptionKeyInput.isEnabled = true
            }
        }
    }

    private fun decryptFile(uri: Uri, keyString: String) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileBytes = inputStream?.readBytes()
            inputStream?.close()

            val keyBytes = hexStringToByteArray(keyString)

            if (fileBytes != null) {
                decryptionStepsLayout.visibility = View.VISIBLE
                decryptionStepsLayout.removeAllViews()

                // Step 1: Verify Decryption Key
                addVerificationStep("Verifying Decryption Key", "Validating RSA-4096 key protection")
                val keyFactory = KeyFactory.getInstance("RSA")
                val privateKeySpec = PKCS8EncodedKeySpec(keyBytes)
                val privateKey = keyFactory.generatePrivate(privateKeySpec)

                // Step 2: Decrypting File
                addVerificationStep("Decrypting File", "Using AES-256-GCM decryption")
                val iv = fileBytes.sliceArray(0..11)
                val encryptedAesKey = fileBytes.sliceArray(12..523) // 512 bytes for 4096-bit RSA
                val encryptedFile = fileBytes.sliceArray(524 until fileBytes.size)

                val rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
                rsaCipher.init(Cipher.DECRYPT_MODE, privateKey)
                val aesKeyBytes = rsaCipher.doFinal(encryptedAesKey)
                val aesKey = SecretKeySpec(aesKeyBytes, "AES")

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val gcmParameterSpec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmParameterSpec)
                decryptedData = cipher.doFinal(encryptedFile)

                // Step 3: Integrity Verified
                addVerificationStep("Integrity Verified", "AES-256-GCM authentication pass")

                downloadDecryptedFileButton.visibility = View.VISIBLE

                Toast.makeText(this, "Decryption Complete!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Decryption failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun addVerificationStep(title: String, subtitle: String) {
        val inflater = LayoutInflater.from(this)
        val stepView = inflater.inflate(R.layout.item_verification_step, decryptionStepsLayout, false)
        val stepTitle: TextView = stepView.findViewById(R.id.step_title)
        val stepSubtitle: TextView = stepView.findViewById(R.id.step_subtitle)
        stepTitle.text = title
        stepSubtitle.text = subtitle
        decryptionStepsLayout.addView(stepView)
    }

    private fun saveFile(uri: Uri, data: ByteArray?) {
        if (data == null) return
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { fos ->
                    fos.write(data)
                }
            }
            Toast.makeText(this, "File saved successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
