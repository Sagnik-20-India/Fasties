package com.example.fasties.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.fasties.databinding.ActivityAddressSelectionBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AddressSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddressSelectionBinding
    private lateinit var adapter: ArrayAdapter<String>
    private var addressList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddressSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadAddresses()

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, addressList)
        binding.addressListView.adapter = adapter

        // 🔹 Select Address
        binding.addressListView.setOnItemClickListener { _, _, position, _ ->
            val selectedAddress = addressList[position]
            val resultIntent = Intent()
            resultIntent.putExtra("selected_address", selectedAddress)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        // 🔹 Long Click: Edit or Delete
        binding.addressListView.setOnItemLongClickListener { _, _, position, _ ->
            val options = arrayOf("Edit", "Delete")

            AlertDialog.Builder(this)
                .setTitle("Choose Action")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> showAddressDialog(isEditing = true, editIndex = position)
                        1 -> {
                            addressList.removeAt(position)
                            saveAddresses()
                            adapter.notifyDataSetChanged()
                            Toast.makeText(this, "Address deleted", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .show()
            true
        }

        // 🔹 Add New Address Button
        binding.addAddressButton.setOnClickListener {
            showAddressDialog()
        }
    }

    private fun showAddressDialog(isEditing: Boolean = false, editIndex: Int = -1) {
        val dialogEditText = EditText(this)
        dialogEditText.hint = "Enter address"

        if (isEditing && editIndex >= 0) {
            dialogEditText.setText(addressList[editIndex])
        }

        AlertDialog.Builder(this)
            .setTitle(if (isEditing) "Edit Address" else "Add New Address")
            .setView(dialogEditText)
            .setPositiveButton("Save") { _, _ ->
                val address = dialogEditText.text.toString().trim()
                if (address.isNotEmpty()) {
                    if (isEditing && editIndex >= 0) {
                        addressList[editIndex] = address
                    } else {
                        addressList.add(address)
                    }
                    saveAddresses()
                    adapter.notifyDataSetChanged()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadAddresses() {
        val sharedPref = getSharedPreferences("user_addresses", Context.MODE_PRIVATE)
        val json = sharedPref.getString("address_list", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<String>>() {}.type
            addressList = Gson().fromJson(json, type)
        }
    }

    private fun saveAddresses() {
        val sharedPref = getSharedPreferences("user_addresses", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val json = Gson().toJson(addressList)
        editor.putString("address_list", json)
        editor.apply()
    }
}