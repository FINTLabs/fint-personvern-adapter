package no.fint.personvern.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.hateos.Embedded;
import no.fint.model.metamodell.kompleksedatatyper.Attributt;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.kodeverk.PersonopplysningResource;
import org.apache.commons.text.WordUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

@Service
@Slf4j
public class PersonopplysningService {
    private final RestTemplate restTemplate;

    @Value("${fint.metamodell}")
    private String metamodellUri;

    @Getter
    private final ConcurrentNavigableMap<String, FintLinks> personopplysningResources = new ConcurrentSkipListMap<>();

    public PersonopplysningService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void getAllPersonopplysning(Event<FintLinks> responseEvent) {
        if (personopplysningResources.isEmpty()) {
            getPersonopplysninger();
        }

        personopplysningResources.values().forEach(responseEvent::addData);
    }

    @Scheduled(initialDelay = 1000L, fixedRate = 3600000L)
    public void getPersonopplysninger() {
        log.info("Updating personopplysning resources");

        ResponseEntity<Embedded> exchange = restTemplate.exchange(metamodellUri,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Embedded>() {
                }
        );

        exchange.getBody()
                .getEmbedded()
                .getEntries()
                .stream()
                .flatMap(k -> k.getAttributter().stream().map(a -> toPersonopplysning(a, k.getId().getIdentifikatorverdi())))
                .forEach(personopplysningResource -> personopplysningResources.putIfAbsent(personopplysningResource.getSystemId().getIdentifikatorverdi(), personopplysningResource));

        log.info("{} personopplysning resources", personopplysningResources.size());
    }

    private PersonopplysningResource toPersonopplysning(Attributt attributt, String klasseId) {
        PersonopplysningResource personopplysningResource = new PersonopplysningResource();

        personopplysningResource.setPassiv(false);
        Identifikator systemId = new Identifikator();
        systemId.setIdentifikatorverdi(klasseId + "." + attributt.getNavn().toLowerCase());
        personopplysningResource.setSystemId(systemId);
        personopplysningResource.setKode(attributt.getNavn().toLowerCase());
        personopplysningResource.setNavn(WordUtils.capitalize(attributt.getNavn()));

        return personopplysningResource;
    }
}