// ui/fragment/AddEditFragment.kt

package com.example.subtrack.ui.fragment

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import com.example.subtrack.R
import com.example.subtrack.SubTrackApplication
import com.example.subtrack.data.local.entity.Subscription
import com.example.subtrack.databinding.FragmentAddEditBinding
import com.example.subtrack.ui.viewmodel.SubscriptionViewModel
import com.example.subtrack.util.CurrencyUtils
import com.example.subtrack.util.DateUtils
import kotlinx.coroutines.launch
import java.util.Calendar

class AddEditFragment : Fragment() {

    private var _binding: FragmentAddEditBinding? = null
    private val binding get() = _binding!!

    // navArgs() reads the 'subscriptionId' argument we declared in nav_graph.xml
    // null means "add new", any string means "edit that ID"
    private val args: AddEditFragmentArgs by navArgs()

    private val viewModel: SubscriptionViewModel by viewModels {
        SubscriptionViewModel.Factory(
            requireActivity().application,
            (requireActivity().application as SubTrackApplication).repository
        )
    }

    // These hold form state
    private var selectedRenewalDate: Long = 0L
    private var selectedColor: String = "#6750A4"   // default to Material purple
    private var selectedBillingCycle: String = "MONTHLY"

    // The subscription being edited (null if we're creating a new one)
    private var existingSubscription: Subscription? = null

    // Colors available in the color picker row
    private val colorOptions = listOf(
        "#F44336", "#FF9800", "#FFC107",
        "#4CAF50", "#009688", "#2196F3",
        "#9C27B0", "#E91E63"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupDropdowns()
        setupColorPicker()
        setupDatePicker()
        setupBillingToggle()

        // Decide: are we in "edit" mode or "add" mode?
        if (args.subscriptionId != null) {
            loadExistingSubscription(args.subscriptionId!!)
        }

        binding.btnSave.setOnClickListener { saveSubscription() }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        // Change title depending on mode
        binding.toolbar.title = if (args.subscriptionId == null)
            getString(R.string.add_subscription)
        else
            getString(R.string.edit)  // "Edit" when modifying
    }

    private fun setupDropdowns() {
        // Category dropdown
        val categories = listOf(
            "Entertainment", "Productivity", "Health & Fitness",
            "News & Media", "Gaming", "Education", "Cloud Storage", "Other"
        )
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.actvCategory.setAdapter(categoryAdapter)

        // Currency dropdown
        val currencyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            CurrencyUtils.supportedCurrencies
        )
        binding.actvCurrency.setAdapter(currencyAdapter)
        binding.actvCurrency.setText("USD", false) // default selection
    }

    private fun setupColorPicker() {
        colorOptions.forEach { hexColor ->
            // Create a circular colored dot view for each color option
            val dotView = View(requireContext()).apply {
                val sizePx = resources.getDimensionPixelSize(R.dimen.color_dot_size)
                layoutParams = ViewGroup.MarginLayoutParams(sizePx, sizePx).apply {
                    marginEnd = resources.getDimensionPixelSize(R.dimen.color_dot_margin)
                }

                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(hexColor))
                }

                setOnClickListener { selectColor(hexColor) }
            }

            binding.colorPickerContainer.addView(dotView)
        }

        // Select the default color on startup
        selectColor(selectedColor)
    }

    private fun selectColor(hexColor: String) {
        selectedColor = hexColor

        // Update each dot: selected one gets a ring, others are plain
        for (i in 0 until binding.colorPickerContainer.childCount) {
            val dot = binding.colorPickerContainer.getChildAt(i)
            val color = colorOptions[i]
            (dot.background as? GradientDrawable)?.apply {
                setColor(Color.parseColor(color))
                if (color == hexColor) {
                    // Draw a ring (stroke) around the selected dot
                    setStroke(6, requireContext().getColor(R.color.md_theme_light_primary))
                } else {
                    setStroke(0, Color.TRANSPARENT)
                }
            }
        }
    }

    private fun setupDatePicker() {
        // Tapping anywhere on the date field opens the DatePickerDialog
        binding.tilRenewalDate.setEndIconOnClickListener { showDatePicker() }
        binding.etRenewalDate.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        val calendar = if (selectedRenewalDate != 0L) {
            DateUtils.timestampToCalendar(selectedRenewalDate)
        } else {
            Calendar.getInstance()
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                // Build a Calendar for the chosen date, set to noon to avoid timezone issues
                val chosen = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedRenewalDate = chosen.timeInMillis
                // Show the formatted date in the text field
                binding.etRenewalDate.setText(DateUtils.formatDate(selectedRenewalDate))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            // Don't allow selecting dates in the past
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    private fun setupBillingToggle() {
        binding.toggleBillingCycle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedBillingCycle = if (checkedId == R.id.btnMonthly) "MONTHLY" else "YEARLY"
            }
        }
        // Select "Monthly" by default
        binding.btnMonthly.isChecked = true
    }

    // If editing an existing subscription, load its data into all the form fields
    private fun loadExistingSubscription(id: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.subscriptions.collect { list ->
                val sub = list.find { it.id == id }
                if (sub != null && existingSubscription == null) {
                    existingSubscription = sub
                    populateForm(sub)
                }
            }
        }
    }

    private fun populateForm(sub: Subscription) {
        binding.etName.setText(sub.name)
        binding.etPrice.setText(sub.price.toString())
        binding.actvCurrency.setText(sub.currency, false)
        binding.actvCategory.setText(sub.category, false)
        selectedRenewalDate = sub.renewalDate
        binding.etRenewalDate.setText(DateUtils.formatDate(sub.renewalDate))
        selectedColor = sub.color
        selectColor(sub.color)
        selectedBillingCycle = sub.billingCycle
        if (sub.billingCycle == "MONTHLY") {
            binding.btnMonthly.isChecked = true
        } else {
            binding.btnYearly.isChecked = true
        }
        binding.switchReminder.isChecked = true
    }

    private fun saveSubscription() {
        // ── Validation ──
        val name = binding.etName.text.toString().trim()
        val priceText = binding.etPrice.text.toString().trim()
        val currency = binding.actvCurrency.text.toString().trim()
        val category = binding.actvCategory.text.toString().trim()

        if (!validateForm(name, priceText, currency, category)) return

        val price = priceText.toDouble()

        val subscription = Subscription(
            id           = existingSubscription?.id ?: java.util.UUID.randomUUID().toString(),
            name         = name,
            price        = price,
            currency     = currency,
            billingCycle = selectedBillingCycle,
            renewalDate  = selectedRenewalDate,
            category     = category.ifEmpty { "Other" },
            color        = selectedColor
        )

        if (existingSubscription == null) {
            viewModel.addSubscription(subscription)
            Snackbar.make(binding.root, "$name added!", Snackbar.LENGTH_SHORT).show()
        } else {
            viewModel.updateSubscription(subscription)
            Snackbar.make(binding.root, "$name updated!", Snackbar.LENGTH_SHORT).show()
        }

        findNavController().navigateUp()
    }

    // Returns true if everything is valid, false + sets error messages if not
    private fun validateForm(name: String, priceText: String, currency: String, category: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.error_name_required)
            isValid = false
        } else {
            binding.tilName.error = null
        }

        if (priceText.isEmpty()) {
            binding.tilPrice.error = getString(R.string.error_price_required)
            isValid = false
        } else if (priceText.toDoubleOrNull() == null) {
            binding.tilPrice.error = getString(R.string.error_price_invalid)
            isValid = false
        } else {
            binding.tilPrice.error = null
        }

        if (selectedRenewalDate == 0L) {
            binding.tilRenewalDate.error = getString(R.string.error_date_required)
            isValid = false
        } else {
            binding.tilRenewalDate.error = null
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}