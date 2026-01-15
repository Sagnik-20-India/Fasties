package com.example.fasties.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fasties.adapters.CartAdapter
import com.example.fasties.databinding.ActivityCartBinding
import com.example.fasties.managers.CartManager

class CartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCartBinding
    private lateinit var adapter: CartAdapter
    private val ADDRESS_REQUEST_CODE = 1001

    private var isCouponApplied = false
    private var discountPercent = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView
        adapter = CartAdapter(CartManager.getCartItems().toMutableList()) { updatedList ->
            CartManager.updateCart(updatedList)
            updateTotal()
        }

        binding.cartRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.cartRecyclerView.adapter = adapter

        // Coupon Apply/Remove Logic
        binding.applyCouponButton.setOnClickListener {
            val enteredCode = binding.couponEditText.text.toString().trim()
            if (!isCouponApplied) {
                if (enteredCode.equals("SAGNIK20", ignoreCase = true)) {
                    discountPercent = 20
                    isCouponApplied = true
                    binding.applyCouponButton.text = "REMOVE"
                    Toast.makeText(this, "20% discount applied!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Invalid coupon code", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Remove coupon
                isCouponApplied = false
                discountPercent = 0
                binding.applyCouponButton.text = "APPLY"
                binding.couponEditText.setText("")
                Toast.makeText(this, "Coupon removed", Toast.LENGTH_SHORT).show()
            }
            updateTotal()
        }

        binding.selectAddressButton.setOnClickListener {
            val intent = Intent(this, AddressSelectionActivity::class.java)
            startActivityForResult(intent, ADDRESS_REQUEST_CODE)
        }

        binding.checkoutButton.setOnClickListener {
            val selectedAddress = binding.selectedAddressTextView.text.toString()
            if (selectedAddress.isBlank() || selectedAddress == "No address selected") {
                Toast.makeText(this, "Please select an address before proceeding", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, CheckoutActivity::class.java)
                intent.putExtra("selected_address", selectedAddress)
                startActivity(intent)
            }
        }

        updateTotal()
        binding.checkoutButton.isEnabled = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ADDRESS_REQUEST_CODE && resultCode == RESULT_OK) {
            val selectedAddress = data?.getStringExtra("selected_address")
            if (!selectedAddress.isNullOrBlank()) {
                binding.selectedAddressTextView.text = selectedAddress
                binding.checkoutButton.isEnabled = true
            }
        }
    }

    private fun updateTotal() {
        val baseTotal = CartManager.getTotalPrice()
        val finalTotal = if (isCouponApplied && discountPercent > 0) {
            (baseTotal * (100 - discountPercent)) / 100
        } else {
            baseTotal
        }
        binding.totalPriceText.text = "Total:   ₹$finalTotal"
    }
}





//package com.example.fasties.activities
//
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.fasties.adapters.CartAdapter
//import com.example.fasties.databinding.ActivityCartBinding
//import com.example.fasties.managers.CartManager
//
//class CartActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityCartBinding
//    private lateinit var adapter: CartAdapter
//    private val ADDRESS_REQUEST_CODE = 1001
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityCartBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // ✅ Setup RecyclerView
//        adapter = CartAdapter(CartManager.getCartItems().toMutableList()) { updatedList ->
//            // Update CartManager data and refresh total
//            CartManager.updateCart(updatedList)
//            updateTotal()
//        }
//
//        binding.cartRecyclerView.layoutManager = LinearLayoutManager(this)
//        binding.cartRecyclerView.adapter = adapter
//
//        binding.selectAddressButton.setOnClickListener {
//            val intent = Intent(this, AddressSelectionActivity::class.java)
//            startActivityForResult(intent, ADDRESS_REQUEST_CODE) // Request code
//        }
//
//        binding.checkoutButton.setOnClickListener {
//            val selectedAddress = binding.selectedAddressTextView.text.toString()
//
//            if (selectedAddress.isBlank() || selectedAddress == "No address selected") {
//                Toast.makeText(this, "Please select an address before proceeding", Toast.LENGTH_SHORT).show()
//            } else {
//                val intent = Intent(this, CheckoutActivity::class.java)
//                intent.putExtra("selected_address", selectedAddress)
//                startActivity(intent)
//            }
//        }
//
//        updateTotal()
//        binding.checkoutButton.isEnabled = false
//
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == ADDRESS_REQUEST_CODE && resultCode == RESULT_OK) {
//            val selectedAddress = data?.getStringExtra("selected_address")
//            if (!selectedAddress.isNullOrBlank()) {
//                binding.selectedAddressTextView.text = selectedAddress
//                binding.checkoutButton.isEnabled = true
//            }
//        }
//    }
//
//    private fun updateTotal() {
//        binding.totalPriceText.text = "Total:   ₹${CartManager.getTotalPrice()}"
//    }
//
//}
