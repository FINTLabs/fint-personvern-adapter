package no.fint.personvern.handler.kodeverk;

import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.metamodell.kompleksedatatyper.Attributt;
import no.fint.model.personvern.kodeverk.KodeverkActions;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.metamodell.KlasseResource;
import no.fint.model.resource.metamodell.KlasseResources;
import no.fint.model.resource.personvern.kodeverk.PersonopplysningResource;
import no.fint.personvern.service.Handler;
import org.apache.commons.text.WordUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class PersonopplysningHandler implements Handler {
    private final RestTemplate restTemplate;

    @Value("${fint.metamodell}")
    private String metamodellUri;

    private List<PersonopplysningResource> personopplysningResources = new ArrayList<>();

    public PersonopplysningHandler(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void accept(Event<FintLinks> event) {
        getPersonopplysningResources(event);
    }

    private void getPersonopplysningResources(Event<FintLinks> event) {
        if (personopplysningResources.isEmpty()) {
            updatePersonopplysningResources();
        }

        personopplysningResources.forEach(event::addData);

        event.setResponseStatus(ResponseStatus.ACCEPTED);
    }

    @Scheduled(initialDelay = 1000L, fixedRate = 3600000L)
    private void updatePersonopplysningResources() {
        log.info("Updating PersonopplysningResource");

        try {
            KlasseResources resources = restTemplate.getForObject(metamodellUri, KlasseResources.class);

            personopplysningResources = resources.getContent()
                    .stream()
                    .flatMap(klasseResource -> klasseResource.getAttributter()
                            .stream()
                            .map(attributt -> toPersonopplysningResource(attributt, klasseResource)))
                    .collect(Collectors.toList());

            log.info("Found {} PersonopplysningResource", personopplysningResources.size());

        } catch (RestClientException ex) {
            log.error(ex.getMessage());
        }
    }

    private PersonopplysningResource toPersonopplysningResource(Attributt attributt, KlasseResource klasseResource) {
        PersonopplysningResource personopplysningResource = new PersonopplysningResource();

        Identifikator systemId = new Identifikator();
        systemId.setIdentifikatorverdi(klasseResource.getId().getIdentifikatorverdi() + "." + attributt.getNavn().toLowerCase());
        personopplysningResource.setSystemId(systemId);
        personopplysningResource.setKode(attributt.getNavn().toLowerCase());
        personopplysningResource.setNavn(WordUtils.capitalize(attributt.getNavn()));
        personopplysningResource.setPassiv(false);

        return personopplysningResource;
    }

    @Override
    public Set<String> actions() {
        return Collections.singleton(KodeverkActions.GET_ALL_PERSONOPPLYSNING.name());
    }
}