package no.fintlabs.personvern.samtykke

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import no.fintlabs.personvern.ResourceKey
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity(name = "Behandling")
class BehandlingEntity(
    @EmbeddedId
    var key: ResourceKey,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    var resource: BehandlingResource,

    var lastModifiedDate: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun toEntity(resource: BehandlingResource, orgId: String) =
            BehandlingEntity(ResourceKey(orgId, resource.systemId.identifikatorverdi), resource)
    }
}
