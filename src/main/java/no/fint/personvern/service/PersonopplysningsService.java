package no.fint.personvern.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.hateos.Embedded;
import no.fint.model.metamodell.kompleksedatatyper.Attributt;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.kodeverk.PersonopplysningResource;
import no.fint.personvern.AppProps;
import org.apache.commons.text.WordUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PersonopplysningsService {

    private final RestTemplate restTemplate;
    private final AppProps props;

    @Getter
    private List<FintLinks> personopplysningResourceList;

    public PersonopplysningsService(
            RestTemplate restTemplate,
            AppProps props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    @Scheduled(initialDelay = 1000L, fixedRate = 3600000L)
    public void getPersonopplysninger() {
        log.info("Getting personopplysning list");
        ResponseEntity<Embedded> exchange = restTemplate.exchange(props.getMetamodellUri(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Embedded>() {
                }
        );

        personopplysningResourceList = exchange.getBody()
                .getEmbedded()
                .getEntries()
                .stream()
                .flatMap(k -> k.getAttributter().stream().map(a -> toPersonopplysning(a, k.getId().getIdentifikatorverdi())))
                .collect(Collectors.toList());

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