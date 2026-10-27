package com.mmgpt.shoplistgpt

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.doubleClick
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Double-tapping a category name on the Plan "Categories" page (first page) or
 * the Shop summary page (last page) must jump to that category's page,
 * wherever on the row the user taps.
 */
@RunWith(AndroidJUnit4::class)
class CategoryDoubleTapTest {

    @get:Rule
    val savedState = SavedAppStateRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        setCategories(CATEGORY_COUNT)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    // ---- Plan mode: first page lists categories; category i is page i + 1 ----

    @Test
    fun plan_doubleTapOnName_opensCategory() {
        openPlan()
        onView(rowPart(R.id.tvName, "Cat 2")).perform(doubleClick())
        assertOnCategoryPage(expectedPage = 3, heading = "Cat 2")
    }

    @Test
    fun plan_doubleTapOnCheckbox_opensCategory() {
        openPlan()
        onView(rowPart(R.id.chkPurchased, "Cat 2")).perform(doubleClick())
        assertOnCategoryPage(expectedPage = 3, heading = "Cat 2")
    }

    @Test
    fun plan_doubleTapOnEachRowName_opensMatchingCategory() {
        openPlan()
        for (i in 0 until CATEGORY_COUNT) {
            onView(rowPart(R.id.tvName, "Cat $i")).perform(doubleClick())
            assertOnCategoryPage(expectedPage = i + 1, heading = "Cat $i")
            scenario.setPage(0)
        }
    }

    // ---- Shop mode: last page summarises categories; category i is page i ----

    @Test
    fun shop_doubleTapOnName_opensCategory() {
        openShopSummary()
        onView(rowPart(R.id.tvName, "Cat 2")).perform(doubleClick())
        assertOnCategoryPage(expectedPage = 2, heading = "Cat 2")
    }

    @Test
    fun shop_doubleTapOnCount_opensCategory() {
        openShopSummary()
        onView(rowPart(R.id.tvCount, "Cat 2")).perform(doubleClick())
        assertOnCategoryPage(expectedPage = 2, heading = "Cat 2")
    }

    @Test
    fun shop_doubleTapOnEachRowName_opensMatchingCategory() {
        openShopSummary()
        for (i in 0 until CATEGORY_COUNT) {
            onView(rowPart(R.id.tvName, "Cat $i")).perform(doubleClick())
            assertOnCategoryPage(expectedPage = i, heading = "Cat $i")
            scenario.setPage(CATEGORY_COUNT)
        }
    }

    @Test
    fun shop_afterCategoryCountChanges_doubleTapOpensCategory() {
        // Visit Shop with fewer categories so the pager caches its pages,
        // then grow the list (as an import would) and come back.
        setCategories(2)
        openShopSummary()
        onView(withId(R.id.action_home)).perform(click())
        setCategories(CATEGORY_COUNT)
        openShopSummary()
        onView(rowPart(R.id.tvName, "Cat 2")).perform(doubleClick())
        assertOnCategoryPage(expectedPage = 2, heading = "Cat 2")
    }

    // ---- helpers ----

    private fun setCategories(count: Int) {
        scenario.setCategories((0 until count).map { i ->
            ShoppingCategory(
                id = 100L + i,
                heading = "Cat $i",
                items = listOf(ShoppingItem(1000L + i, "Item $i"))
            )
        })
    }

    private fun openPlan() {
        onView(withId(R.id.btnPlan)).perform(click())
        scenario.setPage(0)
    }

    private fun openShopSummary() {
        onView(withId(R.id.btnShop)).perform(click())
        scenario.onActivity { activity ->
            val pager = activity.findViewById<ViewPager2>(R.id.viewPager)
            pager.setCurrentItem(pager.adapter!!.itemCount - 1, false)
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    /** A view inside the on-screen category row whose name is [name]. */
    private fun rowPart(id: Int, name: String): Matcher<View> = allOf(
        withId(id),
        isDisplayed(),
        isDescendantOfA(withId(R.id.recyclerView)),
        if (id == R.id.tvName) withText(name) else hasSibling(withText(name))
    )

    private fun assertOnCategoryPage(expectedPage: Int, heading: String) {
        scenario.waitForPagerToSettle()
        assertEquals("ViewPager page after double-tapping $heading", expectedPage, scenario.currentPage())
        onView(allOf(withId(R.id.tvHeading), isDisplayed())).check(matches(withText(heading)))
    }

    companion object {
        private const val CATEGORY_COUNT = 5
    }
}
