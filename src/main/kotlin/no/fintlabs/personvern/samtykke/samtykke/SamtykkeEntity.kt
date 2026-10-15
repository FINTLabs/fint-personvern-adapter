package no.fintlabs.personvern.samtykke.samtykke

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import no.novari.fint.model.resource.personvern.samtykke.SamtykkeResource
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity(name = "Samtykke")
class SamtykkeEntity(
    @Id
    var id: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    var resource: SamtykkeResource,

    var orgId: String? = null,

    var lastModifiedDate: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun toEntity(resource: SamtykkeResource, orgId: String?) = SamtykkeEntity(
            id = resource.systemId.identifikatorverdi,
            resource = resource,
            orgId = orgId,
        )
    }
}
