package no.fint.personvern.service;

import no.fint.event.model.Event;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource;
import no.fint.personvern.exception.CollectionNotFoundException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
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
    private final MongoTemplate mongoTemplate;

    public KodeverkService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void getAllBehandlingsgrunnlag(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();

        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }

        Query query = new Query().restrict(BehandlingsgrunnlagResource.class);

        List<BehandlingsgrunnlagResource> resources = mongoTemplate.find(query, BehandlingsgrunnlagResource.class, orgId);

        if (resources.isEmpty()) {
            createBehandlingsgrunnlag(orgId);
            resources = mongoTemplate.find(query, BehandlingsgrunnlagResource.class, orgId);
        }

        resources.forEach(responseEvent::addData);
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
            mongoTemplate.insert(resource, orgId);
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
