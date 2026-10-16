package app.lang.shared.presentation

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

/** Short Anki-like interval label: 1m, 6m, 3h, 8d, 1.5mo, 2y. */
fun formatInterval(interval: Duration): String = when {
    interval < 1.hours -> "${max(1, (interval.inWholeSeconds / 60.0).roundToInt())}m"
    interval < 1.days -> "${(interval.inWholeMinutes / 60.0).roundToInt()}h"
    interval < 30.days -> "${interval.inWholeDays}d"
    interval < 365.days -> "${oneDecimal(interval.inWholeDays / 30.0)}mo"
    else -> "${oneDecimal(interval.inWholeDays / 365.0)}y"
}

private fun oneDecimal(value: Double): String {
    val tenths = (value * 10).roundToInt()
    return if (tenths % 10 == 0) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
}
