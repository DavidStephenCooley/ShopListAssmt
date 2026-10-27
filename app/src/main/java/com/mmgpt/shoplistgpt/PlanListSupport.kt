package com.mmgpt.shoplistgpt

import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.mmgpt.shoplistgpt.adapter.ItemAdapter

// Shared by the two Plan mode lists: the categories (PlanCategoriesFragment)
// and a category's items (PlanCategoryFragment). Both show their entries
// between an "Add..." row at the top and one at the bottom, so list position
// p holds entry p - 1.

/** [entries] with an "Add..." row above and below, ready to submit to the adapter. */
fun withAddRows(entries: List<ShoppingItem>): List<ShoppingItem> =
    listOf(ShoppingItem(-1, "", isPlaceholder = true)) +
        entries +
        ShoppingItem(-2, "", isPlaceholder = true)

/**
 * Drag-to-reorder for a Plan list, started from a row's drag handle. The
 * "Add..." rows can't be moved or dropped onto. [onReorder] receives entry
 * indices (not list positions).
 */
class PlanListDragCallback(
    private val onReorder: (from: Int, to: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val adapter = recyclerView.adapter as ItemAdapter
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition
        val itemFrom = adapter.currentList.getOrNull(from)
        val itemTo = adapter.currentList.getOrNull(to)
        if (itemFrom?.isPlaceholder == true || itemTo?.isPlaceholder == true) return false
        onReorder(from - 1, to - 1)
        adapter.moveItem(from, to)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun isLongPressDragEnabled(): Boolean = false
}

/**
 * Asks for a new name for a Plan list entry, starting from [current]. Calls
 * [onRename] with the trimmed text when OK is pressed, unless it is blank.
 */
fun Fragment.showEditDialog(current: String, onRename: (String) -> Unit) {
    val input = EditText(requireContext()).apply { setText(current) }
    AlertDialog.Builder(requireContext())
        .setTitle(getString(R.string.edit))
        .setView(input)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                onRename(text)
            }
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}
