package com.example.fasties.models

data class CartItem(
    val foodItem: FoodItem,
    var quantity: Int = 1
)
