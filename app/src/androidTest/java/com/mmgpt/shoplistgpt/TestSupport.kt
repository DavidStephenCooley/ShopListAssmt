package com.mmgpt.shoplistgpt

import android.content.Context
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.util.TreeIterables
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import org.hamcrest.Matcher
import org.junit.rules.ExternalResource

/**
 * Starts each test from an empty list and puts back whatever list was on the
 * device afterwards, so running the tests doesn't destroy it.
 */
class SavedAppStateRule : ExternalResource() {
    private var savedState: String? = null

    private val prefs
        get() = InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("state", Context.MODE_PRIVATE)

    override fun before() {
        savedState = prefs.getString("state", null)
        prefs.edit().clear().commit()
    }

    override fun after() {
        prefs.edit().apply {
            if (savedState == null) clear() else putString("state", savedState)
        }.commit()
    }
}

fun ActivityScenario<MainActivity>.viewModel(): ShoppingListViewModel {
    lateinit var vm: ShoppingListViewModel
    onActivity { activity ->
        vm = ViewModelProvider(
            activity,
            ViewModelProvider.AndroidViewModelFactory.getInstance(activity.application)
        )[ShoppingListViewModel::class.java]
    }
    return vm
}

fun ActivityScenario<MainActivity>.categories(): List<ShoppingCategory> {
    val vm = viewModel()
    var list: List<ShoppingCategory> = emptyList()
    onActivity { list = vm.categories.value.orEmpty() }
    return list
}

fun ActivityScenario<MainActivity>.setCategories(list: List<ShoppingCategory>) {
    val vm = viewModel()
    onActivity { vm.setCategories(list) }
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
}

fun ActivityScenario<MainActivity>.currentPage(): Int {
    var page = -1
    onActivity { page = it.findViewById<ViewPager2>(R.id.viewPager).currentItem }
    return page
}

fun ActivityScenario<MainActivity>.setPage(page: Int) {
    onActivity { it.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(page, false) }
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
}

/** Espresso doesn't wait for ViewPager2's smooth scroll, so poll until it stops. */
fun ActivityScenario<MainActivity>.waitForPagerToSettle() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val deadline = System.currentTimeMillis() + 5_000
    while (System.currentTimeMillis() < deadline) {
        instrumentation.waitForIdleSync()
        var idle = false
        onActivity {
            idle = it.findViewById<ViewPager2>(R.id.viewPager).scrollState ==
                ViewPager2.SCROLL_STATE_IDLE
        }
        if (idle) return
        Thread.sleep(50)
    }
}

/**
 * Performs [action] on the child [childId] of the RecyclerView row at adapter
 * [position]; a negative position counts from the end (-1 is the last row).
 */
fun onRow(position: Int, childId: Int, action: ViewAction): ViewAction = object : ViewAction {
    override fun getConstraints(): Matcher<View> = isDisplayed()

    override fun getDescription() = "on row $position child $childId: ${action.description}"

    override fun perform(uiController: UiController, view: View) {
        val rv = view as RecyclerView
        val count = rv.adapter!!.itemCount
        val row = if (position < 0) count + position else position
        val holder = rv.findViewHolderForAdapterPosition(row)
            ?: error("Row $row of $count is not laid out")
        action.perform(uiController, holder.itemView.findViewById(childId))
        uiController.loopMainThreadUntilIdle()
    }
}

/**
 * Presses the first displayed view matching [handle] (a drag handle) and drags
 * it, in small steps, until the dragged row passes the centre of the row
 * containing the first displayed view matching [target], then lets go.
 *
 * Touches are injected with UiAutomation from the test thread instead of an
 * Espresso ViewAction: while a row is dragged near the list's edge,
 * ItemTouchHelper keeps posting auto-scroll callbacks, so the main thread is
 * never idle and Espresso would wait forever to inject the finger-up.
 */
fun ActivityScenario<MainActivity>.drag(handle: Matcher<View>, target: Matcher<View>) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    var start = FloatArray(2)
    var end = FloatArray(2)
    onActivity { activity ->
        // isDisplayed() skips the ViewPager's off-screen pages.
        val views = TreeIterables.breadthFirstViewTraversal(activity.window.decorView)
            .filter { isDisplayed().matches(it) }
        val handleView = views.first { handle.matches(it) }
        val targetRow = rowOf(views.first { target.matches(it) })
        start = centreOnScreen(handleView)
        val targetY = centreOnScreen(targetRow)[1]
        // Go a third of a row past the target's centre so ItemTouchHelper swaps.
        val overshoot = targetRow.height / 3f * if (targetY > start[1]) 1 else -1
        end = floatArrayOf(start[0], targetY + overshoot)
    }

    val downTime = SystemClock.uptimeMillis()
    fun inject(action: Int, x: Float, y: Float) {
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        instrumentation.uiAutomation.injectInputEvent(event, true)
        event.recycle()
    }

    inject(MotionEvent.ACTION_DOWN, start[0], start[1])
    val steps = 20
    for (i in 1..steps) {
        Thread.sleep(20)
        inject(MotionEvent.ACTION_MOVE, start[0], start[1] + (end[1] - start[1]) * i / steps)
    }
    Thread.sleep(100)
    inject(MotionEvent.ACTION_UP, end[0], end[1])
    instrumentation.waitForIdleSync()
    Thread.sleep(300) // let the drop animation and list update finish
    instrumentation.waitForIdleSync()
}

private fun rowOf(v: View): View {
    var row = v
    while (row.parent !is RecyclerView) row = row.parent as View
    return row
}

private fun centreOnScreen(v: View): FloatArray {
    val xy = IntArray(2)
    v.getLocationOnScreen(xy)
    return floatArrayOf(xy[0] + v.width / 2f, xy[1] + v.height / 2f)
}
