package no.fint.utilities;

import no.fint.event.model.Event;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;

import java.util.Collections;
import java.util.Date;

public class ObjectFactory {


    public static Event<FintLinks> createSamtykkeEvent() {
        return createSamtykkeEvent("test.no");
    }

    public static Event<FintLinks> createSamtykkeEvent(String orgId) {
        SamtykkeResource samtykkeResource = new SamtykkeResource();
        Periode periode = new Periode();
        periode.setStart(new Date());
        samtykkeResource.setGyldighetsperiode(periode);

        return createEvent(samtykkeResource, orgId);
    }

    public static Event<FintLinks> createTjenesteEvent() {
        return createTjenesteEvent("test.no");
    }

    public static Event<FintLinks> createTjenesteEvent(String orgId) {
        TjenesteResource tjenesteResource = new TjenesteResource();
        tjenesteResource.setNavn("Test");

        return createEvent(tjenesteResource, orgId);
    }

    public static Event<FintLinks> createEvent(FintLinks fintLinks, String orgId) {
        Event<FintLinks> event = new Event<>();
        event.setOrgId(orgId);
        event.setData(Collections.singletonList(fintLinks));
        event.setQuery("systemid/123");

        return event;
    }


    public static Event<FintLinks> createBehandlingEvent() {
        return createBehandlingEvent("test.no");
    }

    public static Event<FintLinks> createBehandlingEvent(String orgId) {
        BehandlingResource behandlingResource = new BehandlingResource();

        return createEvent(behandlingResource, orgId);
    }
}
