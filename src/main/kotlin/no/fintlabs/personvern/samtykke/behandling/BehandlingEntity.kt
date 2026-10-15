package no.fintlabs.personvern.samtykke.behandling

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity(name = "Behandling")
class BehandlingEntity(
    @Id
    var id: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    var resource: BehandlingResource,

    var orgId: String? = null,

    var lastModifiedDate: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun toEntity(resource: BehandlingResource, orgId: String?) = BehandlingEntity(
            id = resource.systemId.identifikatorverdi,
            resource = resource,
            orgId = orgId,
        )
    }
}
