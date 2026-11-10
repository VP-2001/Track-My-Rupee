package com.example.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps standard transaction category names to clean, readable Material icons.
 */
fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase().trim()) {
        "food", "food & dining", "groceries", "dining", "restaurants", "snacks" -> Icons.Default.Fastfood
        "shopping", "retail", "clothing", "electronics" -> Icons.Default.ShoppingBag
        "transport", "transportation", "travel", "fuel", "cab", "auto", "metro", "bus" -> Icons.Default.DirectionsBus
        "bills", "utilities", "electricity", "water", "gas", "recharge" -> Icons.Default.Receipt
        "entertainment", "movies", "games", "streaming" -> Icons.Default.Movie
        "health", "medical", "pharmacy", "doctor" -> Icons.Default.LocalHospital
        "education", "books", "courses", "tuition" -> Icons.Default.School
        "home", "rent", "housing", "maintenance" -> Icons.Default.Home
        "internet", "broadband", "wifi", "mobile" -> Icons.Default.Wifi
        "flights", "vacation", "hotel" -> Icons.Default.Flight
        "gifts", "donation", "charity" -> Icons.Default.CardGiftcard
        else -> Icons.Default.Category
    }
}
