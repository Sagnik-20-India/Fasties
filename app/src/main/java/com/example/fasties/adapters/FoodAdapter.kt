//package com.example.fasties
//
//class FoodAdapter {
//}

// adapters/FoodAdapter.kt
package com.example.fasties.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.fasties.databinding.ItemFoodBinding
import com.example.fasties.models.CartStorage
import com.example.fasties.models.FoodItem
import com.example.fasties.managers.CartManager

class FoodAdapter(private var foodList: List<FoodItem>) :
    RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    inner class FoodViewHolder(val binding: ItemFoodBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemFoodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val item = foodList[position]
        val context = holder.itemView.context

        holder.binding.foodName.text = item.name
        holder.binding.foodDescription.text = item.description
        holder.binding.foodPrice.text = "₹${item.price}"
        holder.binding.foodImage.setImageResource(item.imageResId)

        holder.binding.addToCartButton.setOnClickListener {
            CartManager.addToCart(item)  // ✅ Correct manager
            Toast.makeText(context, "${item.name} added to cart!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = foodList.size

    fun updateList(newList: List<FoodItem>) {
        foodList = newList
        notifyDataSetChanged()
    }
}

