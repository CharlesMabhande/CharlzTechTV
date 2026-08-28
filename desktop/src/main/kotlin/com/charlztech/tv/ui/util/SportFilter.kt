package com.charlztech.tv.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsGolf
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material.icons.filled.SportsRugby
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.outlined.Event
import androidx.compose.ui.graphics.vector.ImageVector
import com.charlztech.tv.data.model.EventStatus
import com.charlztech.tv.data.model.LiveEventUi

sealed class SportFilter {
    data object All : SportFilter()
    data object AllLive : SportFilter()
    data class Sport(val category: String) : SportFilter()
}

fun sportCategoryIcon(category: String): ImageVector {
    val key = category.lowercase().trim()
    return when {
        "cricket" in key -> Icons.Default.SportsCricket
        "football" in key && "american" !in key -> Icons.Default.SportsSoccer
        "soccer" in key -> Icons.Default.SportsSoccer
        "baseball" in key -> Icons.Default.SportsBaseball
        "basketball" in key -> Icons.Default.SportsBasketball
        "tennis" in key -> Icons.Default.SportsTennis
        "hockey" in key -> Icons.Default.SportsHockey
        "rugby" in key -> Icons.Default.SportsRugby
        "golf" in key -> Icons.Default.SportsGolf
        "volleyball" in key -> Icons.Default.SportsVolleyball
        "mma" in key || "ufc" in key || "boxing" in key || "fight" in key -> Icons.Default.SportsMartialArts
        "american football" in key || key == "nfl" -> Icons.Default.SportsFootball
        "racing" in key || "f1" in key || "motorsport" in key || "motor" in key -> Icons.Default.SportsMotorsports
        "esport" in key || "gaming" in key -> Icons.Default.SportsEsports
        else -> Icons.Outlined.Event
    }
}

fun orderedSportCategories(grouped: Map<String, List<LiveEventUi>>): List<String> {
    return grouped.entries
        .sortedWith(
            compareByDescending<Map.Entry<String, List<LiveEventUi>>> { entry ->
                entry.value.count { it.status == EventStatus.LIVE }
            }.thenBy { it.key }
        )
        .map { it.key }
}

fun applySportFilter(
    grouped: Map<String, List<LiveEventUi>>,
    filter: SportFilter
): Map<String, List<LiveEventUi>> {
    return when (filter) {
        is SportFilter.All -> grouped
        is SportFilter.AllLive -> grouped.mapValues { (_, events) ->
            events.filter { it.status == EventStatus.LIVE }
        }.filterValues { it.isNotEmpty() }
        is SportFilter.Sport -> grouped.filterKeys { category ->
            category.equals(filter.category, ignoreCase = true)
        }
    }
}

fun isSportFilterSelected(filter: SportFilter, candidate: SportFilter): Boolean {
    return when {
        filter is SportFilter.All && candidate is SportFilter.All -> true
        filter is SportFilter.AllLive && candidate is SportFilter.AllLive -> true
        filter is SportFilter.Sport && candidate is SportFilter.Sport ->
            filter.category.equals(candidate.category, ignoreCase = true)
        else -> false
    }
}

fun SportFilter.isDefault(): Boolean = this is SportFilter.All

fun SportFilter.displayLabel(): String = when (this) {
    is SportFilter.All -> "All"
    is SportFilter.AllLive -> "Live"
    is SportFilter.Sport -> category
}
