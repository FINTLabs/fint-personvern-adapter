package no.fintlabs.personvern.samtykke.tjeneste

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import no.novari.fint.model.resource.personvern.samtykke.TjenesteResource
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity(name = "Tjeneste")
class TjenesteEntity(
    @Id
    var id: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    var resource: TjenesteResource,

    var orgId: String? = null,

    var lastModifiedDate: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun toEntity(resource: TjenesteResource, orgId: String?) = TjenesteEntity(
            id = resource.systemId.identifikatorverdi,
            resource = resource,
            orgId = orgId,
        )
    }
}
