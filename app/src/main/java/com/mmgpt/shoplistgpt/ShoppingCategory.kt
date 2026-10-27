package com.mmgpt.shoplistgpt

data class ShoppingCategory(
    val id: Long,
    val heading: String,
    val items: List<ShoppingItem>
) {
    /** Total number of real items excluding headings and placeholders. */
    val totalCount: Int
        get() = items.count { !it.isPlaceholder && !it.isHeading }

    /** Number of items that *are* checked off. */
    val remainingCount: Int
        get() = items.count {
            it.isChecked && !it.isPlaceholder && !it.isHeading
        }

    /** Number of items that are flagged. */
    val flaggedCount: Int
        get() = items.count {
            it.isFlagged && !it.isPlaceholder && !it.isHeading
        }
}
