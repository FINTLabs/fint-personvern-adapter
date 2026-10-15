package no.fintlabs.adapter.core.event

import no.fintlabs.adapter.core.sync.ResourceDefinition
import no.fintlabs.adapter.models.event.RequestFintEvent

/**
 * Answers write events for one resource. Optional: a capability without a handler is full-sync only.
 */
interface EventHandler<T : Any> : ResourceDefinition<T> {
    /** The org the event is for is in [RequestFintEvent.getOrgId]. Throwing is reported as [EventResult.Failed]. */
    fun handle(event: RequestFintEvent, resource: T): EventResult<T>
}

sealed interface EventResult<out T : Any> {
    /** The resource as stored, which is what the client gets back. */
    data class Saved<T : Any>(val resource: T) : EventResult<T>

    /** The request was understood but not acceptable; reported to the client as a 400. */
    data class Rejected(val reason: String) : EventResult<Nothing>

    /** Something went wrong on the adapter side; reported to the client as a 500. */
    data class Failed(val message: String) : EventResult<Nothing>
}
