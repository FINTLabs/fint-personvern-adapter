package no.fintlabs.personvern

import no.fintlabs.ResourceVerifierService
import no.fintlabs.adapter.core.event.EventResult
import no.fintlabs.adapter.models.event.RequestFintEvent
import no.fintlabs.adapter.operation.OperationType
import no.fintlabs.personvern.samtykke.BehandlingAdapter
import no.fintlabs.personvern.samtykke.BehandlingJpaRepository
import no.fintlabs.personvern.samtykke.SamtykkeAdapter
import no.fintlabs.personvern.samtykke.SamtykkeJpaRepository
import no.fintlabs.personvern.samtykke.TjenesteAdapter
import no.fintlabs.personvern.samtykke.TjenesteJpaRepository
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.resource.Link
import no.novari.fint.model.resource.personvern.samtykke.BehandlingResource
import no.novari.fint.model.resource.personvern.samtykke.SamtykkeResource
import no.novari.fint.model.resource.personvern.samtykke.TjenesteResource
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class SamtykkeAdaptersTest(
    @Autowired private val behandlinger: BehandlingJpaRepository,
    @Autowired private val samtykker: SamtykkeJpaRepository,
    @Autowired private val tjenester: TjenesteJpaRepository,
) : PostgresJpaTest() {

    private val verifier = ResourceVerifierService()
    private val behandlingAdapter by lazy { BehandlingAdapter(behandlinger, verifier) }
    private val samtykkeAdapter by lazy { SamtykkeAdapter(samtykker, verifier) }
    private val tjenesteAdapter by lazy { TjenesteAdapter(tjenester, verifier) }

    private fun event(orgId: String, type: OperationType = OperationType.UPDATE) = RequestFintEvent().apply {
        this.orgId = orgId
        operationType = type
        corrId = "corr"
    }

    private fun behandling(id: String, formal: String = "Formal") = BehandlingResource().apply {
        aktiv = true
        this.formal = formal
        systemId = Identifikator().apply { identifikatorverdi = id }
        addBehandlingsgrunnlag(Link("a"))
        addTjeneste(Link("t"))
        addPersonopplysning(Link("p"))
    }

    @Test
    fun `the same systemId is stored per org and only synced to that org`() {
        assertIs<EventResult.Saved<BehandlingResource>>(behandlingAdapter.handle(event("rogfk.no"), behandling("b1", "Rogaland")))
        assertIs<EventResult.Saved<BehandlingResource>>(behandlingAdapter.handle(event("agderfk.no"), behandling("b1", "Agder")))

        assertEquals(listOf("Rogaland"), behandlingAdapter.resources("rogfk.no").map { it.formal })
        assertEquals(listOf("Agder"), behandlingAdapter.resources("agderfk.no").map { it.formal })
        assertEquals(emptyList(), behandlingAdapter.resources("vestfoldfylke.no"))
    }

    @Test
    fun `an update replaces the stored resource`() {
        behandlingAdapter.handle(event("rogfk.no"), behandling("b1", "First"))
        behandlingAdapter.handle(event("rogfk.no"), behandling("b1", "Second"))

        assertEquals(listOf("Second"), behandlingAdapter.resources("rogfk.no").map { it.formal })
    }

    @Test
    fun `an incomplete behandling is rejected and not stored`() {
        val result = behandlingAdapter.handle(event("rogfk.no"), BehandlingResource().apply { formal = "x" })

        assertIs<EventResult.Rejected>(result)
        assertEquals(emptyList(), behandlingAdapter.resources("rogfk.no"))
    }

    @Test
    fun `a samtykke create is stored without verification and gets opprettet`() {
        val resource = SamtykkeResource().apply { systemId = Identifikator().apply { identifikatorverdi = "s1" } }

        val result = samtykkeAdapter.handle(event("rogfk.no", OperationType.CREATE), resource)

        val saved = assertIs<EventResult.Saved<SamtykkeResource>>(result)
        assertNotNull(saved.resource.opprettet)
        assertEquals("s1", samtykkeAdapter.resources("rogfk.no").single().systemId.identifikatorverdi)
    }

    @Test
    fun `a samtykke update without required fields is rejected`() {
        val resource = SamtykkeResource().apply { systemId = Identifikator().apply { identifikatorverdi = "s1" } }

        assertIs<EventResult.Rejected>(samtykkeAdapter.handle(event("rogfk.no", OperationType.UPDATE), resource))
    }

    @Test
    fun `a tjeneste needs a name`() {
        val withName = TjenesteResource().apply {
            systemId = Identifikator().apply { identifikatorverdi = "t1" }
            navn = "Tjeneste"
        }
        val withoutName = TjenesteResource().apply { systemId = Identifikator().apply { identifikatorverdi = "t2" } }

        assertIs<EventResult.Saved<TjenesteResource>>(tjenesteAdapter.handle(event("rogfk.no"), withName))
        assertIs<EventResult.Rejected>(tjenesteAdapter.handle(event("rogfk.no"), withoutName))
        assertEquals(listOf("t1"), tjenesteAdapter.resources("rogfk.no").map { it.systemId.identifikatorverdi })
    }
}
