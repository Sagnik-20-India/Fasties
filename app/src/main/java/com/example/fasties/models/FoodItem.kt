//package models
//
//class FoodItem {
//}

// models/FoodItem.kt
package com.example.fasties.models

data class FoodItem(
    val name: String,
    val description: String,
    val price: Double,
    val imageResId: Int,
    val category: String,
    var quantityInCart: Int = 0
)
