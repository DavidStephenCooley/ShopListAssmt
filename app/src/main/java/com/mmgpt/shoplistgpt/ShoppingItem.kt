package com.mmgpt.shoplistgpt

data class ShoppingItem(
    val id: Long,
    val name: String,
    var isChecked: Boolean = false,
    var isFlagged: Boolean = false,
    val isHeading: Boolean = false,
    val isStruck: Boolean = false,
    /** Indicates this entry is a UI placeholder (e.g. the \"Add...\" row). */
    val isPlaceholder: Boolean = false
)
