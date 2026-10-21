package no.fintlabs.adapter.core.event

import no.fintlabs.adapter.core.sync.ResourceDefinition
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.operation.OperationType
import no.novari.fint.model.resource.FintResource

/**
 * Answers write events for one resource. Optional: a resource without a handler is full-sync only.
 */
interface EventHandler<T : FintResource> : ResourceDefinition<T> {
    /**
     * The operations this handler answers. They are registered as the contract's event capabilities, so the
     * gateway only serves these; anything else (DELETE, VALIDATE, READ) is never seen by [handle].
     */
    val operations: Set<OperationType>
        get() = setOf(OperationType.CREATE, OperationType.UPDATE)

    /** The org the event is for is in [RequestFintEvent.getOrgId]. Throwing is reported as [EventResult.Failed]. */
    fun handle(
        event: RequestFintEvent,
        resource: T,
    ): EventResult<T>
}

sealed interface EventResult<out T : Any> {
    /** The resource as stored, which is what the client gets back. */
    data class Saved<T : Any>(
        val resource: T,
    ) : EventResult<T>

    /** The request was understood but not acceptable; reported to the client as a 400. */
    data class Rejected(
        val reason: String,
    ) : EventResult<Nothing>

    /** Something went wrong on the adapter side; reported to the client as a 500. */
    data class Failed(
        val message: String,
    ) : EventResult<Nothing>
}
