package com.example.fasties.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fasties.databinding.ItemCartBinding
import com.example.fasties.models.CartItem
import com.example.fasties.models.FoodItem

class CartAdapter(
    private var cartList: MutableList<CartItem>,
    private val onCartUpdated: (List<CartItem>) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(val binding: ItemCartBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartList[position]

        with(holder.binding) {
            cartItemName.text = item.foodItem.name
            cartItemQty.text = "Qty: ${item.quantity}"
            cartItemPrice.text = "₹${item.foodItem.price * item.quantity}"

            increaseButton.setOnClickListener {
                item.quantity++
                cartItemQty.text = "Qty: ${item.quantity}"
                cartItemPrice.text = "₹${item.foodItem.price * item.quantity}"
                onCartUpdated(cartList)
            }

            removeButton.setOnClickListener {
                val currentPosition = holder.adapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    if (item.quantity > 1) {
                        item.quantity--
                        cartItemQty.text = "Qty: ${item.quantity}"
                        cartItemPrice.text = "₹${item.foodItem.price * item.quantity}"
                    } else {
                        cartList.removeAt(currentPosition)
                        notifyItemRemoved(currentPosition)
                    }
                    onCartUpdated(cartList)
                }
            }
        }
    }

    override fun getItemCount() = cartList.size
}
