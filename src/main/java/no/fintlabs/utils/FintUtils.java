package no.fintlabs.utils;

import no.fint.model.felles.kompleksedatatyper.Identifikator;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FintUtils {

    public Identifikator createNewSystemId() {
        Identifikator identifikator = new Identifikator();
        identifikator.setIdentifikatorverdi(UUID.randomUUID().toString());
        return identifikator;
    }

}
