package com.roei.stagemate.data.interfaces

import com.roei.stagemate.data.models.Ticket

// Click handler for ticket list items, used by TicketAdapter.
interface TicketCallback {
    fun onTicketClicked(ticket: Ticket, position: Int)
    fun onDownloadClicked(ticket: Ticket, position: Int)
    fun onRateClicked(ticket: Ticket, position: Int)
}
