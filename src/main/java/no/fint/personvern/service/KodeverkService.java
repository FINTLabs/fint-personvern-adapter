package no.fint.personvern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource;
import no.fint.personvern.wrapper.Wrapper;
import no.fint.personvern.wrapper.WrapperDocument;
import no.fint.personvern.wrapper.WrapperDocumentRepository;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class KodeverkService {
    //private final MongoService mongoService;
    private final PersonopplysningsService personopplysningsService;
    protected final Wrapper wrapper;
    private final WrapperDocumentRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public KodeverkService(PersonopplysningsService personopplysningsService, Wrapper wrapper, WrapperDocumentRepository repository) {
        this.personopplysningsService = personopplysningsService;
        this.wrapper = wrapper;
        this.repository = repository;
    }

    public List<BehandlingsgrunnlagResource> getAllBehandlingsgrunnlag(Event<FintLinks> responseEvent) {
        if (existsInDatabase(responseEvent.getOrgId())) {
            createBehandlingsgrunnlag(responseEvent.getOrgId());
        }

        return repository
                .findByOrgIdAndType(responseEvent.getOrgId(), BehandlingsgrunnlagResource.class.getCanonicalName())
                .stream()
                .map(WrapperDocument::getValue)
                .map(d -> objectMapper.convertValue(d, BehandlingsgrunnlagResource.class))
                .collect(Collectors.toList());
    }

    public void getAllPersonopplysning(Event<FintLinks> responseEvent) {
        responseEvent.setData(personopplysningsService.getPersonopplysningResourceList());
        responseEvent.setResponseStatus(ResponseStatus.ACCEPTED);

    }

    private boolean existsInDatabase(   String orgId) {
        return repository.findByOrgIdAndType(orgId, BehandlingsgrunnlagResource.class.getCanonicalName()).size() > 0;
    }

    private void createBehandlingsgrunnlag(String orgId) {
        behandlingsgrunnlag.forEach((kode, navn) -> {
            BehandlingsgrunnlagResource resource = new BehandlingsgrunnlagResource();
            Identifikator systemId = new Identifikator();
            systemId.setIdentifikatorverdi(kode);
            resource.setSystemId(systemId);
            resource.setKode(kode);
            resource.setNavn(navn);
            resource.setPassiv(false);
            Periode gyldighetsperiode = new Periode();
            gyldighetsperiode.setStart(Date.from(Instant.parse("2018-07-20T00:00:00.00Z")));
            resource.setGyldighetsperiode(gyldighetsperiode);
            repository.insert(wrapper.wrap(resource, BehandlingsgrunnlagResource.class, orgId));
        });
    }

    private static final Map<String, String> behandlingsgrunnlag = Stream.of(
            new AbstractMap.SimpleImmutableEntry<>("A", "Samtykke"),
            new AbstractMap.SimpleImmutableEntry<>("B", "Nødvendig for å oppfylle en avtale"),
            new AbstractMap.SimpleImmutableEntry<>("C", "Nødvendig for å oppfylle en rettslig plikt"),
            new AbstractMap.SimpleImmutableEntry<>("D", "Nødvendig for å beskytte vitale interesser"),
            new AbstractMap.SimpleImmutableEntry<>("E", "Nødvendig for å utføre en oppgave i offentlig interesse eller utøve offentlig myndighet"),
            new AbstractMap.SimpleImmutableEntry<>("F", "Nødvendig for å ivareta legitime interesser - interesseavveiing"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
}
