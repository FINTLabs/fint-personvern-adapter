package no.fintlabs.personvern.samtykke

import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import no.fintlabs.personvern.ResourceKey
import no.novari.fint.model.resource.personvern.samtykke.TjenesteResource
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity(name = "Tjeneste")
class TjenesteEntity(
    @EmbeddedId
    var key: ResourceKey,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    var resource: TjenesteResource,

    var lastModifiedDate: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun toEntity(resource: TjenesteResource, orgId: String) =
            TjenesteEntity(ResourceKey(orgId, resource.systemId.identifikatorverdi), resource)
    }
}
