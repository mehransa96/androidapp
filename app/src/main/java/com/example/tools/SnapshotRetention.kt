package com.example.tools

import java.time.LocalDate

/** Which automatic safety copies to keep. Pure logic, covered by unit tests. */
object SnapshotRetention {
    data class Item(val date: LocalDate, val createdAt: Long, val records: Int)

    /**
     * The newest copy of each of the [days] most recent days, plus the copy with the most records
     * (the newest one if several tie), so real data is never pushed out by a nearly empty state.
     */
    fun select(items: List<Item>, days: Int): Set<Item> {
        val perDay = items.groupBy { it.date }
            .values
            .map { sameDay -> sameDay.maxBy { it.createdAt } }
            .sortedByDescending { it.date }
            .take(days)
        val largest = items.maxWithOrNull(compareBy<Item> { it.records }.thenBy { it.createdAt })
        return (perDay + listOfNotNull(largest)).toSet()
    }
}
