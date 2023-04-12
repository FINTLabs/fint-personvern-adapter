package no.fintlabs.personvern.utility;

import no.fint.model.felles.kompleksedatatyper.Identifikator;

import java.util.UUID;

public class FintUtilities {

    public static Identifikator createUuidSystemId() {
        Identifikator systemId = new Identifikator();
        systemId.setIdentifikatorverdi(UUID.randomUUID().toString());

        return systemId;
    }
}
