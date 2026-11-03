package cz.julienika.reportcam.helpers

import android.app.Activity
import android.view.View
import androidx.test.core.app.ActivityScenario

/** Polls [condition] on the main thread until it holds, failing with [what] after [timeoutMs]. */
fun <A : Activity> ActivityScenario<A>.waitFor(
    what: String,
    timeoutMs: Long = 15_000,
    condition: (A) -> Boolean,
) {
    val end = System.currentTimeMillis() + timeoutMs
    while (System.currentTimeMillis() < end) {
        var ok = false
        onActivity { ok = condition(it) }
        if (ok) return
        Thread.sleep(100)
    }
    throw AssertionError("timed out waiting for $what")
}

fun Activity.view(id: Int): View = findViewById(id)
