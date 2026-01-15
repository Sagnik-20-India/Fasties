package com.example.fasties.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.fasties.databinding.ActivityCheckoutBinding

class CheckoutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCheckoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val allMainOptions = listOf(
            binding.paymentCOD,
            binding.paymentUPI,
            binding.paymentNetbanking,
            binding.paymentCredit,
            binding.paymentDebit
        )

        allMainOptions.forEach { button ->
            button.setOnClickListener {
                allMainOptions.forEach { it.isChecked = false }
                button.isChecked = true

                // Show/hide UPI layout
                if (button.id == binding.paymentUPI.id) {
                    binding.upiOptionsLayout.visibility = View.VISIBLE
                } else {
                    binding.upiOptionsLayout.visibility = View.GONE
                    binding.upiOptions.clearCheck() // ensures only one UPI method selected
                    binding.upiIdEditText.text.clear()
                }
            }
        }

        // Handle manual selection to enforce single selection in upiOptions
        val upiRadioButtons = listOf(
            binding.upiGpay,
            binding.upiBhim,
            binding.upiPhonePe,
            binding.upiID
        )

        upiRadioButtons.forEach { radio ->
            radio.setOnClickListener {
                upiRadioButtons.forEach { it.isChecked = false }
                radio.isChecked = true
            }
        }

        binding.confirmPaymentButton.setOnClickListener {
            val selectedButton = allMainOptions.find { it.isChecked }

            if (selectedButton == null) {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val method = when (selectedButton.id) {
                binding.paymentCOD.id -> "Cash on Delivery"
                binding.paymentUPI.id -> {
                    val selectedUpi = upiRadioButtons.find { it.isChecked }
                    val enteredUpiId = binding.upiIdEditText.text.toString()

                    if (selectedUpi == null && enteredUpiId.isBlank()) {
                        Toast.makeText(this, "Please select a UPI method or enter UPI ID", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val upiMethod = selectedUpi?.text?.toString() ?: enteredUpiId
                    "UPI ($upiMethod)"
                }
                binding.paymentNetbanking.id -> "Netbanking"
                binding.paymentCredit.id -> "Credit Card"
                binding.paymentDebit.id -> "Debit Card"
                else -> "Unknown"
            }

            Toast.makeText(this, "Payment Method: $method selected", Toast.LENGTH_LONG).show()
            val intent = Intent(this, OrderConfirmedActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}