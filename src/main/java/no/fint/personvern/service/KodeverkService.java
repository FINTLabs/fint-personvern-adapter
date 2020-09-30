package no.fint.personvern.service;

import no.fint.event.model.Event;
import no.fint.event.model.ResponseStatus;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource;
import no.fint.personvern.utility.SpringerRepository;
import no.fint.personvern.utility.Wrapper;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class KodeverkService extends SpringerRepository {
    private final MongoService mongoService;
    private final PersonopplysningsService personopplysningsService;
    protected final Wrapper wrapper;


    public KodeverkService(MongoService mongoService, PersonopplysningsService personopplysningsService, Wrapper wrapper) {
        super(wrapper, mongoService.getAppProps(), mongoService.getMongoTemplate());
        this.mongoService = mongoService;
        this.personopplysningsService = personopplysningsService;
        this.wrapper = wrapper;
    }

    public void getAllBehandlingsgrunnlag(Event<FintLinks> responseEvent) {
        if (existsInDatabase(BehandlingsgrunnlagResource.class, responseEvent.getOrgId())) {
            createBehandlingsgrunnlag(responseEvent.getOrgId());
        }
        query(BehandlingsgrunnlagResource.class, responseEvent, responseEvent.getOrgId());
    }

    public void getAllPersonopplysning(Event<FintLinks> responseEvent) {
        responseEvent.setData(personopplysningsService.getPersonopplysningResourceList());
        responseEvent.setResponseStatus(ResponseStatus.ACCEPTED);

    }

    private boolean existsInDatabase(Class<? extends FintLinks> clazz, String orgId) {
        return stream(clazz, orgId).count() == 0;
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
            mongoService.insert(wrapper.wrap(resource, BehandlingsgrunnlagResource.class, orgId));
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
