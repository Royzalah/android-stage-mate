package com.roei.stagemate.data.interfaces

import com.roei.stagemate.data.models.Event

// Click handler for event list items, used by EventAdapter.
interface EventCallback {
    fun onEventClicked(event: Event, position: Int)
    fun onFavoriteClicked(event: Event, position: Int)
    fun onBookNowClicked(event: Event, position: Int)
}
