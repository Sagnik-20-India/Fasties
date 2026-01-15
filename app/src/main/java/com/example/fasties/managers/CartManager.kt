package com.example.fasties.managers

import com.example.fasties.models.CartItem
import com.example.fasties.models.FoodItem

object CartManager {
    private val cartItems = mutableListOf<CartItem>()

    fun addToCart(foodItem: FoodItem) {
        val existing = cartItems.find { it.foodItem.name == foodItem.name }
        if (existing != null) {
            existing.quantity++
        } else {
            cartItems.add(CartItem(foodItem, 1))
        }
    }

    fun getCartItems(): List<CartItem> = cartItems

    fun updateCart(updatedList: List<CartItem>) {
        cartItems.clear()
        cartItems.addAll(updatedList)
    }

    fun getTotalPrice(): Double = cartItems.sumOf { it.foodItem.price * it.quantity }

    fun clearCart() {
        cartItems.clear()
    }
}
