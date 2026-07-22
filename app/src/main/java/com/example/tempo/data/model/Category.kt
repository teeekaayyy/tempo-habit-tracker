package com.example.tempo.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val colorHex: String,
    val iconName: String = "Category"
)

val DefaultCategories = listOf(
    Category("cat_health", "Health & Wellness", "#10B981"),
    Category("cat_prod", "Productivity & Work", "#6366F1"),
    Category("cat_fitness", "Fitness & Exercise", "#F59E0B"),
    Category("cat_mindful", "Mindfulness & Rest", "#8B5CF6"),
    Category("cat_learning", "Learning & Growth", "#EC4899")
)
