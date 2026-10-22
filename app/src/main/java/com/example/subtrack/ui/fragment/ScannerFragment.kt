// ui/fragment/ScannerFragment.kt

package com.example.subtrack.ui.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.example.subtrack.R
import com.example.subtrack.databinding.FragmentScannerBinding

class ScannerFragment : Fragment() {

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    // Detected values (will be sent to AddEditFragment)
    private var detectedPrice: Double? = null
    private var detectedName: String = ""

    // registerForActivityResult replaces the old startActivityForResult.
    // It launches the system image picker and gives us back the chosen URI.
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processImage(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tapping the scan card opens the gallery
        binding.cardScanArea.setOnClickListener {
            pickImage.launch("image/*") // filter to images only
        }

        // "Use This Data" button navigates to Add/Edit pre-filled
        binding.btnUseData.setOnClickListener {
            navigateToAddEditWithData()
        }
    }

    private fun processImage(uri: Uri) {
        // Show the picked image as a preview
        binding.ivScannedImage.setImageURI(uri)
        binding.ivScannedImage.visibility = View.VISIBLE
        binding.layoutScanPrompt.visibility = View.GONE

        // Show spinning progress indicator while ML Kit works
        binding.progressScanning.visibility = View.VISIBLE
        binding.cardResults.visibility = View.GONE
        binding.btnUseData.visibility = View.GONE

        try {
            // Create an InputImage from the URI — ML Kit's input format
            val image = InputImage.fromFilePath(requireContext(), uri)

            // Create the text recognizer (Latin script, works for English receipts)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            // process() runs on a background thread automatically
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    binding.progressScanning.visibility = View.GONE
                    // visionText.text is the full text block detected in the image
                    extractSubscriptionData(visionText.text)
                }
                .addOnFailureListener {
                    binding.progressScanning.visibility = View.GONE
                    binding.layoutScanPrompt.visibility = View.VISIBLE
                    binding.ivScannedImage.visibility = View.GONE
                    com.google.android.material.snackbar.Snackbar
                        .make(binding.root, "Could not read image. Try a clearer photo.", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .show()
                }
        } catch (e: Exception) {
            binding.progressScanning.visibility = View.GONE
        }
    }

    private fun extractSubscriptionData(rawText: String) {
        // ── Price detection using Regex ──
        // Matches: $14.99, $14, 14.99 USD, ₹499, €9.99
        val priceRegex = Regex("""[\$€£₹]?\s*(\d+[.,]\d{2})""")
        val priceMatch = priceRegex.find(rawText)
        detectedPrice = priceMatch?.groupValues?.get(1)
            ?.replace(",", ".")  // handle European format like "9,99"
            ?.toDoubleOrNull()

        // ── Service name detection ──
        // Strategy: look for known service keywords in the text
        val knownServices = listOf(
            "Netflix", "Spotify", "Amazon", "Prime", "YouTube", "Disney",
            "Apple", "Adobe", "Microsoft", "Google", "Hulu", "HBO",
            "Dropbox", "Notion", "Slack", "Zoom", "GitHub"
        )
        detectedName = knownServices.firstOrNull { service ->
            rawText.contains(service, ignoreCase = true)
        } ?: run {
            // Fallback: take the first non-empty line as the service name
            rawText.lines()
                .map { it.trim() }
                .firstOrNull { it.length in 3..30 } // reasonable name length
                ?: ""
        }

        // ── Show results card ──
        binding.cardResults.visibility = View.VISIBLE
        binding.etDetectedName.setText(detectedName)
        binding.etDetectedPrice.setText(detectedPrice?.toString() ?: "")
        binding.btnUseData.visibility = View.VISIBLE
    }

    private fun navigateToAddEditWithData() {
        // Read whatever the user edited in the results card (they may have corrected it)
        val finalName = binding.etDetectedName.text.toString()
        val finalPrice = binding.etDetectedPrice.text.toString().toDoubleOrNull() ?: 0.0

        // Fix: Pass scanned values as a Bundle to AddEditFragment so the form is pre-filled.
        // subscriptionId is null — this is a new subscription being created from a scan.
        val bundle = android.os.Bundle().apply {
            putString("scannedName", finalName)
            putDouble("scannedPrice", finalPrice)
        }
        val action = ScannerFragmentDirections
            .actionScannerToAddEdit(subscriptionId = null)
        findNavController().navigate(action.actionId, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}