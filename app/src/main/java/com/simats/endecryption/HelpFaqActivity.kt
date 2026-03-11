package com.simats.endecryption

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.simats.endecryption.databinding.ActivityHelpFaqBinding

class HelpFaqActivity : BaseActivity() {

    private lateinit var binding: ActivityHelpFaqBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpFaqBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFaqItems()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener { finish() }
        
        binding.openSecurityGuideButton.setOnClickListener {
            startActivity(Intent(this, SecurityGuideActivity::class.java))
        }
    }

    private fun setupFaqItems() {
        // ABOUT THE APP
        setupExpandableItem(
            binding.faqSecureSystem.root,
            "What is Secure File Encryption System?",
            "It is an advanced security application designed to protect your sensitive files using industrial-grade encryption algorithms like AES and RSA."
        )

        setupExpandableItem(
            binding.faqWhoIsItFor.root,
            "Who is this app for?",
            "This app is for anyone who needs to store or share sensitive documents securely, from personal users to business professionals."
        )

        // USING THE APP
        setupExpandableItem(
            binding.faqHowEncryptionWorks.root,
            "How does file encryption work?",
            "The app uses a secret key to scramble your file's data into an unreadable format. Only the correct key can reverse this process to recover the original file."
        )

        setupExpandableItem(
            binding.faqDecryptionFail.root,
            "Why does decryption fail sometimes?",
            "Decryption usually fails if the wrong key is provided or if the encrypted file has been tampered with or corrupted."
        )

        setupExpandableItem(
            binding.faqFileStorage.root,
            "Where are my encrypted files stored?",
            "By default, encrypted files are saved in the app's secure internal storage or a designated folder on your device, which you can manage in 'Saved Files'."
        )

        // SECURITY & KEYS
        setupExpandableItem(
            binding.faqLostKey.root,
            "What happens if I lose my decryption key?",
            "Due to the nature of high-level encryption, if you lose your key, the data cannot be recovered. We recommend backing up your keys in a safe place."
        )

        setupExpandableItem(
            binding.faqTamperDetection.root,
            "How does tamper detection work?",
            "The app generates a digital fingerprint (hash) of your file. If even a single bit of the file changes, the fingerprint will no longer match, alerting you to potential tampering."
        )

        // PRIVACY & DATA
        setupExpandableItem(
            binding.faqDataShared.root,
            "Is my data shared?",
            "No. We follow a zero-knowledge policy. Your files and encryption keys never leave your device and are never shared with our servers."
        )

        setupExpandableItem(
            binding.faqDeleteData.root,
            "Can I delete all my data?",
            "Yes, you can wipe all application data, including saved files and history, from the Privacy & Security settings."
        )
    }

    private fun setupExpandableItem(view: View, question: String, answer: String) {
        val questionTv = view.findViewById<TextView>(R.id.faq_question)
        val answerTv = view.findViewById<TextView>(R.id.faq_answer)
        val arrowIv = view.findViewById<ImageView>(R.id.arrow_icon)
        val header = view.findViewById<View>(R.id.header_layout)

        questionTv.text = question
        answerTv.text = answer

        header.setOnClickListener {
            if (answerTv.visibility == View.GONE) {
                answerTv.visibility = View.VISIBLE
                arrowIv.setImageResource(R.drawable.ic_arrow_up)
            } else {
                answerTv.visibility = View.GONE
                arrowIv.setImageResource(R.drawable.ic_arrow_down)
            }
        }
    }
}
