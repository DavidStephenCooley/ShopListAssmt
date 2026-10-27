package com.mmgpt.shoplistgpt

import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Plan mode has two editable lists with the same behaviour: the Categories
 * page (categories) and each category's page (its items). Both show an
 * "Add..." row above and below the entries, can be reordered by dragging the
 * handle (but not onto an "Add..." row), and have edit and delete buttons.
 * The tests are written once here and run against each list by the
 * subclasses below.
 */
abstract class PlanListEditingTest {

    @get:Rule
    val savedState = SavedAppStateRule()

    protected lateinit var scenario: ActivityScenario<MainActivity>

    /** Pager page showing the list under test. */
    protected abstract val page: Int

    /** Names currently in the list under test, in order, read from the ViewModel. */
    protected abstract fun names(): List<String>

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.setCategories((0 until 3).map { c ->
            ShoppingCategory(
                id = 100L + c,
                heading = "Cat $c",
                items = (0 until 3).map { i ->
                    ShoppingItem(id = 1000L + c * 10 + i, name = "Cat $c item $i")
                }
            )
        })
        onView(withId(R.id.btnPlan)).perform(click())
        scenario.setPage(page)
        scenario.waitForPagerToSettle()
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    private val initial get() = names()

    // ---- "Add..." rows ----

    @Test
    fun addRowAtTop_insertsAtStart() {
        val before = initial
        addViaRow(position = 0, name = "New")
        assertEquals(listOf("New") + before, names())
    }

    @Test
    fun addRowAtBottom_appendsAtEnd() {
        val before = initial
        addViaRow(position = -1, name = "New")
        assertEquals(before + "New", names())
    }

    @Test
    fun addRow_ignoresBlankText() {
        val before = initial
        addViaRow(position = 0, name = "   ")
        assertEquals(before, names())
    }

    // ---- drag to reorder ----

    @Test
    fun dragDown_movesEntryDown() {
        val (a, b, c) = initial
        dragHandleOf(a, onto = withText(b))
        assertEquals(listOf(b, a, c), names())
        assertListShows(listOf(b, a, c))
    }

    @Test
    fun dragUp_movesEntryUp() {
        val (a, b, c) = initial
        dragHandleOf(c, onto = withText(b))
        assertEquals(listOf(a, c, b), names())
        assertListShows(listOf(a, c, b))
    }

    @Test
    fun dragOntoTopAddRow_doesNothing() {
        val before = initial
        dragHandleOf(before.first(), onto = addRow(0))
        assertEquals(before, names())
        assertListShows(before)
    }

    @Test
    fun dragOntoBottomAddRow_doesNothing() {
        val before = initial
        dragHandleOf(before.last(), onto = addRow(-1))
        assertEquals(before, names())
        assertListShows(before)
    }

    // ---- edit and delete buttons ----

    @Test
    fun edit_renamesThatEntry() {
        val (a, b, c) = initial
        editEntry(b, newText = "  Renamed  ", confirm = true)
        assertEquals(listOf(a, "Renamed", c), names())
    }

    @Test
    fun edit_withBlankText_keepsName() {
        val before = initial
        editEntry(before[1], newText = "   ", confirm = true)
        assertEquals(before, names())
    }

    @Test
    fun edit_cancelled_keepsName() {
        val before = initial
        editEntry(before[1], newText = "Renamed", confirm = false)
        assertEquals(before, names())
    }

    @Test
    fun delete_removesThatEntry() {
        val (a, b, c) = initial
        onView(rowChild(R.id.btnDelete, b)).perform(click())
        assertEquals(listOf(a, c), names())
    }

    @Test
    fun list_showsEntriesBetweenAddRows() {
        assertListShows(initial)
    }

    // ---- helpers ----

    /** The on-screen list is an "Add..." row, then [expected] in order, then an "Add..." row. */
    private fun assertListShows(expected: List<String>) {
        onView(displayedList()).check { view, noViewFound ->
            if (noViewFound != null) throw noViewFound
            val rv = view as RecyclerView
            val rows = rv.adapter!!.itemCount
            assertEquals("Add row + entries + Add row", expected.size + 2, rows)
            for (row in 0 until rows) {
                val itemView = rv.findViewHolderForAdapterPosition(row)!!.itemView
                if (row == 0 || row == rows - 1) {
                    assertNotNull("Add row at $row", itemView.findViewById<View>(R.id.etAdd))
                } else {
                    assertEquals(expected[row - 1], itemView.findViewById<TextView>(R.id.tvName).text.toString())
                }
            }
        }
    }

    private fun displayedList() = allOf(withId(R.id.recyclerView), isDisplayed())

    private fun addViaRow(position: Int, name: String) {
        onView(displayedList()).perform(onRow(position, R.id.etAdd, replaceText(name)))
        onView(displayedList()).perform(onRow(position, R.id.btnAdd, click()))
    }

    /** The edit text of the "Add..." row at [position] (negative counts from the end). */
    private fun addRow(position: Int) = object : TypeSafeMatcher<View>() {
        override fun describeTo(description: Description) {
            description.appendText("Add row at $position")
        }

        override fun matchesSafely(view: View): Boolean {
            if (view.id != R.id.etAdd || !view.isShown) return false
            val rv = generateSequence(view.parent) { it.parent }
                .filterIsInstance<RecyclerView>().firstOrNull()
                ?: return false
            val count = rv.adapter?.itemCount ?: return false
            val row = if (position < 0) count + position else position
            return rv.findViewHolderForAdapterPosition(row)?.itemView?.findViewById<View>(R.id.etAdd) === view
        }
    }

    private fun rowChild(id: Int, name: String) =
        allOf(withId(id), hasSibling(withText(name)), isDisplayed())

    private fun dragHandleOf(name: String, onto: Matcher<View>) {
        scenario.drag(handle = rowChild(R.id.dragHandle, name), target = onto)
    }

    private fun editEntry(name: String, newText: String, confirm: Boolean) {
        onView(rowChild(R.id.btnEdit, name)).perform(click())
        onView(isAssignableFrom(EditText::class.java)).inRoot(isDialog())
            .check(matches(withText(name)))
            .perform(replaceText(newText))
        val button = if (confirm) android.R.string.ok else android.R.string.cancel
        onView(withText(button)).inRoot(isDialog()).perform(click())
    }
}

/** Plan mode's first page: the list of categories. */
@RunWith(AndroidJUnit4::class)
class PlanCategoriesEditingTest : PlanListEditingTest() {
    override val page = 0
    override fun names() = scenario.categories().map { it.heading }
}

/** Plan mode's page for "Cat 1": the list of its items. */
@RunWith(AndroidJUnit4::class)
class PlanCategoryItemsEditingTest : PlanListEditingTest() {
    override val page = 2
    override fun names() = scenario.categories()[1].items.map { it.name }
}
