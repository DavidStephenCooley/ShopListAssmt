package com.mmgpt.shoplistgpt

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

import com.mmgpt.shoplistgpt.PlanCategoryFragment
import com.mmgpt.shoplistgpt.PlanCategoriesFragment

class CategoryPagerAdapter(
    activity: FragmentActivity,
    private val viewModel: ShoppingListViewModel,
    private val planMode: Boolean = false
) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int =
        (viewModel.categories.value?.size ?: 0) + 1

    // By default a page's ID is its position, so the pager keeps the fragment
    // it already made for that position. When the number of categories
    // changes, the Shop summary page moves to a new position and its old
    // position would keep showing the summary instead of a category. Including
    // the category count (and mode) in the ID makes the pager rebuild pages
    // whenever the layout changes.
    override fun getItemId(position: Int): Long {
        val size = (viewModel.categories.value?.size ?: 0).toLong()
        val mode = if (planMode) 1L else 0L
        return (mode shl 62) or (size shl 31) or position.toLong()
    }

    override fun containsItem(itemId: Long): Boolean =
        (0 until itemCount).any { getItemId(it) == itemId }

    override fun createFragment(position: Int): Fragment =
        if (planMode) {
            if (position == 0) {
                PlanCategoriesFragment()
            } else {
                PlanCategoryFragment.newInstance(position - 1)
            }
        } else {
            val size = viewModel.categories.value?.size ?: 0
            if (position == size) {
                ShopCategoriesFragment()
            } else {
                CategoryFragment.newInstance(position)
            }
        }
}
