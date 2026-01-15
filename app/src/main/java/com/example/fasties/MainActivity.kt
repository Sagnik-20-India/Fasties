package com.example.fasties

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fasties.activities.CartActivity
import com.example.fasties.activities.LoginActivity
import com.example.fasties.adapters.FoodAdapter
import com.example.fasties.databinding.ActivityMainBinding
import com.example.fasties.models.FoodItem
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fullFoodList: List<FoodItem>
    private lateinit var adapter: FoodAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("FastiesPrefs", MODE_PRIVATE)
        val username = intent.getStringExtra("username") ?: "Guest"
        binding.welcomeTextView.text = "Welcome ${username}!"

        binding.logoutButton.setOnClickListener {
//            prefs.edit().clear().apply()
//            val intent = Intent(this, LoginActivity::class.java)
//            startActivity(intent)
//            finish()
            FirebaseAuth.getInstance().signOut()
            // Clear preferences
            val prefs = getSharedPreferences("FastiesPrefs", MODE_PRIVATE)
            prefs.edit().clear().apply()

            // Navigate to LoginActivity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }


        fullFoodList = listOf(
            //Burgers
            FoodItem("Veggie Burger", "Mashed potato patty with some veggies and cheese", 40.0, R.drawable.vegburger, "Burgers"),
            FoodItem("Eggie Burger", "Poached egg and mashed potato patty with some veggies and cheese", 60.0, R.drawable.eggburger, "Burgers"),
            FoodItem("Royal Paneer Burger", "Grilled paneer with some veggies and cheese", 70.0, R.drawable.paneerburger, "Burgers"),
            FoodItem("Chicken Burger", "Juicy chicken patty with some veggies and cheese", 90.0, R.drawable.chickenburger, "Burgers"),
            FoodItem("Fried Chicken Burger", "Crispy fried chicken with some veggies and cheese", 110.0, R.drawable.friedchickenburger, "Burgers"),

            //Pizza
            FoodItem("Veggie Pizza", "Loaded with fresh vegetables", 60.0, R.drawable.vegpizza, "Pizza"),
            FoodItem("Veg Cheese Burst Pizza", "Cheesy crust with veggies", 80.0, R.drawable.vegcheesepizza, "Pizza"),
            FoodItem("Paneer Pizza", "Cheesy crust with chunks of paneer and veggies", 80.0, R.drawable.paneerpizza, "Pizza"),
            FoodItem("Paneer Cheese Burst Pizza", "Chunks of paneer with veggies", 100.0, R.drawable.paneercheesepizza, "Pizza"),
            FoodItem("Chicken Pizza", "Topped with grilled chicken", 100.0, R.drawable.chickenpizza, "Pizza"),
            FoodItem("Chicken Sausage Pizza", "Juicy chicken sausage chunks with veggies and cheese", 120.0, R.drawable.chickensausagepizza, "Pizza"),
            FoodItem("Chicken Pepperoni Pizza", "Classic chicken pepperoni with veggies and cheese", 120.0, R.drawable.chickenpepperonipizza, "Pizza"),
            FoodItem("Chicken Cheese Burst Pizza", "Cheesy crust with chicken and veggies", 130.0, R.drawable.chickencheesepizza, "Pizza"),

            //Rolls
            FoodItem("Egg Roll", "Stuffed with egg and some veggies", 50.0, R.drawable.eggroll, "Roll"),
            FoodItem("Paneer Roll", "Soft paneer with some veggies", 70.0, R.drawable.paneerroll, "Roll"),
            FoodItem("Chicken Roll", "Grilled chicken in a wrap with some veggies", 80.0, R.drawable.chickenroll, "Roll"),
            FoodItem("Fried Chicken Roll", "Crispy chicken in wrap with some veggies", 100.0, R.drawable.friedchickenroll, "Roll"),

            //Sides
            FoodItem("Fries", "Crispy golden fries", 50.0, R.drawable.fries, "Sides"),
            FoodItem("Spicy Fries", "Hot and spicy fries", 60.0, R.drawable.spicyfries, "Sides"),
            FoodItem("Chicken Popcorn", "Mini fried chicken bites", 80.0, R.drawable.chickenpops, "Sides"),
            FoodItem("Fried Chicken Strips", "Crunchy chicken strips", 100.0, R.drawable.chickenstrips, "Sides"),

            //Desserts
            FoodItem("Red Velvet Cake", "Soft and sweet strawberry flavoured cake", 70.0, R.drawable.redvelvetcake, "Desserts"),
            FoodItem("Choco Lava Cake", "Warm chocolate filled cake", 70.0, R.drawable.chocolavacake, "Desserts"),
            FoodItem("Chocolate Brownie", "Rich chocolate delight drizzled with chocolate syrup and topped with vanilla ice-cream", 90.0, R.drawable.brownie, "Desserts"),
            FoodItem("Black Forest Cake", "Cherries and whipped cream on chocolate-vanilla flavoured cake", 110.0, R.drawable.blackforestcake, "Desserts"),

            //Beverages
            FoodItem("Mineral Water", "Fresh mineral water", 20.0, R.drawable.water, "Beverages"),
            FoodItem("Sprite", "Lemon-flavored cold drink", 40.0, R.drawable.sprite, "Beverages"),
            FoodItem("Coca-Cola", "Classic cola flavor", 40.0, R.drawable.cocacola, "Beverages"),
            FoodItem("Fanta", "Orange-flavored soda", 40.0, R.drawable.fanta, "Beverages"),
            FoodItem("Mountain Dew", "Citrus-flavored soda", 40.0, R.drawable.mountaindew, "Beverages"),
            FoodItem("Limca", "Lime and lemon drink", 40.0, R.drawable.limca, "Beverages"),
            FoodItem("Vanilla Ice Cream Shake", "Creamy vanilla shake topped with ice-cream", 70.0, R.drawable.vanillashake, "Beverages"),
            FoodItem("Chocolate Ice Cream Shake", "Thick chocolate shake topped with ice-cream", 80.0, R.drawable.chocolateshake, "Beverages"),
            FoodItem("Strawberry Ice Cream Shake", "Thick strawberry shake topped with ice-cream", 80.0, R.drawable.strawberryshake, "Beverages")
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FoodAdapter(fullFoodList)
        binding.recyclerView.adapter = adapter

        val cartButton: Button = findViewById(R.id.viewCartButton)
        cartButton.setOnClickListener {
            val intent = Intent(this, CartActivity::class.java)
            startActivity(intent)
        }

        // Category Filters
        binding.btnAll.setOnClickListener {
            adapter.updateList(fullFoodList)
        }
        binding.btnBurger.setOnClickListener { filterList("Burgers") }
        binding.btnPizza.setOnClickListener { filterList("Pizza") }
        binding.btnRoll.setOnClickListener { filterList("Roll") }
        binding.btnSides.setOnClickListener { filterList("Sides") }
        binding.btnDesserts.setOnClickListener { filterList("Desserts") }
        binding.btnBeverages.setOnClickListener { filterList("Beverages") }

    }

    private fun filterList(category: String) {
        val filteredList = fullFoodList.filter { it.category == category }
        adapter = FoodAdapter(filteredList)
        binding.recyclerView.adapter = adapter
    }
}

