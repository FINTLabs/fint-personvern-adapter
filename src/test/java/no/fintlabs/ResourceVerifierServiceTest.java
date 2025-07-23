package no.fintlabs;

import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.Link;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceVerifierServiceTest {

    private ResourceVerifierService verifier;

    @BeforeEach
    void setUp() {
        verifier = new ResourceVerifierService();
    }

    @Test
    void testShouldFailWhenBehandlingResourceHasFieldsThatAreNull() {
        BehandlingResource resource = new BehandlingResource();
        resource.setAktiv(null);
        resource.setFormal("test");

        assertFalse(verifier.verifyBehandlingResource(resource));
    }

    @Test
    void testShouldFailWhenBehandlingResourceHasFieldsThatAreEmpty() {
        BehandlingResource resource = new BehandlingResource();
        resource.setAktiv(true);
        resource.setFormal("");
        resource.setSystemId(new Identifikator() {{
            setIdentifikatorverdi("test");
        }});
        resource.setLinks(null);

        assertFalse(verifier.verifyBehandlingResource(resource));
    }

    @Test
    void testShouldPassWhenAllFieldsArePresentInBehandlingResource() {
        BehandlingResource resource = new BehandlingResource();
        resource.setAktiv(true);
        resource.setFormal("test");
        resource.setSystemId(new Identifikator() {{
            setIdentifikatorverdi("indentifikatorverdi");
        }});

        resource.addBehandlingsgrunnlag(new Link("api.felleskomponent.no"));
        resource.addTjeneste(new Link("api.felleskomponent.no"));
        resource.addPersonopplysning(new Link("api.felleskomponent.no"));

        assertTrue(verifier.verifyBehandlingResource(resource));
    }

    @Test
    void testShouldFailWhenLinksAreEmpty() {
        BehandlingResource resource = new BehandlingResource();
        resource.setAktiv(true);
        resource.setFormal("test");
        resource.setSystemId(new Identifikator() {{
            setIdentifikatorverdi("indentifikatorverdi");
        }});

        resource.addBehandlingsgrunnlag(new Link(""));
        resource.addTjeneste(new Link(""));

        assertFalse(verifier.verifyBehandlingResource(resource));
    }

    @Test
    void testShouldFailWhenLinksAreNull() {
        BehandlingResource resource = new BehandlingResource();
        resource.setAktiv(true);
        resource.setFormal("test");
        resource.setSystemId(new Identifikator() {{
            setIdentifikatorverdi("indentifikatorverdi");
        }});

        resource.addBehandlingsgrunnlag(new Link());
        resource.addTjeneste(new Link());
        resource.addPersonopplysning(new Link());

        assertFalse(verifier.verifyBehandlingResource(resource));
    }

    @Test
    void testShoudPassWhenAllFieldsArePressenInSamtykkeResource(){
        SamtykkeResource resource = new SamtykkeResource();
        resource.setSystemId(new Identifikator() {{setIdentifikatorverdi("indentifikatorverdi");}});
        resource.setOpprettet(new Date(new Date().getTime()));
        resource.setGyldighetsperiode(new Periode(){{setStart(new Date(new Date().getTime()));}});
        resource.addBehandling(new Link("api.felleskomponent.no"));
        resource.addPerson(new Link("api.felleskomponent.no"));

        assertTrue(verifier.verifySamtykkeResource(resource));
    }

    @Test
    void testShoudFailWhenFieldsAreNullInSamtykkeResource(){
        SamtykkeResource resource = new SamtykkeResource();
        resource.setSystemId(new Identifikator() {{setIdentifikatorverdi("indentifikatorverdi");}});
        resource.setOpprettet(new Date(new Date().getTime()));
        resource.setGyldighetsperiode(null);
        resource.addBehandling(new Link("api.felleskomponent.no"));
        resource.addPerson(new Link("api.felleskomponent.no"));

        assertFalse(verifier.verifySamtykkeResource(resource));
    }

    @Test
    void testShouldPassWhenAllFieldsArePressentInTjenesteResource(){
        TjenesteResource resource = new TjenesteResource();
        resource.setSystemId(new Identifikator() {{setIdentifikatorverdi("indentifikatorverdi");}});
        resource.setNavn("testName");
        resource.addBehandling(new Link("api.felleskomponent.no"));

        assertTrue(verifier.verifyTjenesteResource(resource));
    }

    @Test
    void testSouldFailWhenFieldsAreEmptyInTjenesteResource(){
        TjenesteResource resource = new TjenesteResource();
        resource.setSystemId(new Identifikator() {{setIdentifikatorverdi("indentifikatorverdi");}});
        resource.setNavn("");
        resource.addBehandling(new Link("api.felleskomponent.no"));

        assertFalse(verifier.verifyTjenesteResource(resource));
    }

}
