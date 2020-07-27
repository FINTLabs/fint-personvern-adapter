package no.fint.personvern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.event.model.Operation;
import no.fint.model.administrasjon.organisasjon.Organisasjonselement;
import no.fint.model.felles.Person;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.personvern.kodeverk.Behandlingsgrunnlag;
import no.fint.model.personvern.samtykke.*;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.Link;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import no.fint.personvern.exception.CollectionNotFoundException;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.exception.MongoEntryExistsException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SamtykkeService {
    private static final String FINTLabs = "fintlabs.no";

    private final MongoTemplate mongoTemplate;
    private ObjectMapper objectMapper = new ObjectMapper();

    public SamtykkeService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void getAllSamtykke(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();

        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }

        Query query = new Query().restrict(SamtykkeResource.class);

        List<SamtykkeResource> resources = mongoTemplate.find(query, SamtykkeResource.class, orgId);

        if (resources.isEmpty() && orgId.equals(FINTLabs)) {
            createSamtykker(orgId);
            resources = mongoTemplate.find(query, SamtykkeResource.class, orgId);
        }

        resources.forEach(responseEvent::addData);
    }

    public void updateSamtykke(Event<FintLinks> responseEvent) throws MongoEntryExistsException {

        String orgId = responseEvent.getOrgId();
        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }
        SamtykkeResourceWrapper samtykke = objectMapper.convertValue(responseEvent.getData().get(0), SamtykkeResourceWrapper.class);
        if (responseEvent.getOperation() == Operation.CREATE) {
            Query query = new Query().restrict(SamtykkeResourceWrapper.class);
            List<SamtykkeResourceWrapper> resources = mongoTemplate.find(query, SamtykkeResourceWrapper.class, orgId);

            Optional<SamtykkeResourceWrapper> any =
                    resources.stream()
                            .filter(samtykkeResource -> samtykkeResource.getSystemId().getIdentifikatorverdi().equals(samtykke.getSystemId().getIdentifikatorverdi()))
                            .findAny();
            if (any.isPresent()) {
                ArrayList<FintLinks> fintLinks = new ArrayList<>();
                SamtykkeResource samtykkeResource = any.get();
                fintLinks.add(samtykkeResource);
                responseEvent.setData(fintLinks);
                throw new MongoEntryExistsException("Samtykke allready exists: " + responseEvent.getOperation());
            } else {
                mongoTemplate.insert(samtykke, orgId);
            }
        } else if (responseEvent.getOperation() == Operation.UPDATE) {
            String identificator = responseEvent.getQuery();
            String identificatorNumber = identificator.split("/")[1];

            Query query1 = Query.query(Criteria.where("systemId.identifikatorverdi")
                    .is(identificatorNumber
                    ));
            SamtykkeResourceWrapper found = mongoTemplate.findOne(query1
                    , SamtykkeResourceWrapper.class, orgId);

            if (found != null) {
                found.setSystemId(samtykke.getSystemId());
                found.setGyldighetsperiode(samtykke.getGyldighetsperiode());
                found.setOpprettet(samtykke.getOpprettet());
                found.setLinks(samtykke.getLinks());
                mongoTemplate.save(found, orgId);
            } else {
                throw new MongoCantFindDocumentException(
                        "Invalid operation: systemId.identifikatorverdi "
                                + samtykke.getSystemId().getIdentifikatorverdi()
                                + " doesnt exist: "
                                + responseEvent.getOperation());
            }
        } else {
            throw new IllegalArgumentException("Invalid operation: " + responseEvent.getOperation());
        }
    }

    public void getAllTjeneste(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();

        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }

        Query query = new Query().restrict(TjenesteResource.class);

        List<TjenesteResource> resources = mongoTemplate.find(query, TjenesteResource.class, orgId);

        if (resources.isEmpty() && orgId.equals(FINTLabs)) {
            createTjenester(orgId);
            resources = mongoTemplate.find(query, TjenesteResource.class, orgId);
        }

        resources.forEach(responseEvent::addData);
    }

    public void updateTjeneste(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();
        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }
        TjenesteResource tjeneste = objectMapper.convertValue(responseEvent.getData().get(0), TjenesteResource.class);
        if (responseEvent.getOperation() == Operation.CREATE) {
            Query query = new Query().restrict(TjenesteResource.class);
            List<TjenesteResource> resources = mongoTemplate.find(query, TjenesteResource.class, orgId);
            Optional<TjenesteResource> any =
                    resources.stream()
                            .filter(tjenesteResource -> tjenesteResource.getSystemId().equals(tjeneste.getSystemId()))
                            .findAny();
            if (any.isPresent()) {
                throw new MongoEntryExistsException("Tjeneste allready exists: " + responseEvent.getOperation());
            }
            mongoTemplate.insert(tjeneste, orgId);
        } else if (responseEvent.getOperation() == Operation.UPDATE) {
            String identificator = responseEvent.getQuery();
            String identificatorNumber = identificator.split("/")[1];

            Query query1 = Query.query(Criteria.where("systemId.identifikatorverdi")
                    .is(identificatorNumber
                    ));
            TjenesteResource found = mongoTemplate.findOne(query1
                    , TjenesteResource.class, orgId);

            if (found != null) {
                found.setSystemId(tjeneste.getSystemId());
                found.setNavn(tjeneste.getNavn());
                found.setLinks(tjeneste.getLinks());
                mongoTemplate.save(found, orgId);
            } else {
                throw new MongoCantFindDocumentException(
                        "Invalid operation: systemId.identifikatorverdi "
                                + tjeneste.getSystemId().getIdentifikatorverdi()
                                + " doesnt exist: "
                                + responseEvent.getOperation()
                );
            }
        } else {
            throw new IllegalArgumentException("Invalid operation: " + responseEvent.getOperation());
        }

    }

    public void getAllBehandling(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();

        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }

        Query query = new Query().restrict(BehandlingResource.class);

        List<BehandlingResource> resources = mongoTemplate.find(query, BehandlingResource.class, orgId);

        if (resources.isEmpty() && orgId.equals(FINTLabs)) {
            createBehandlinger(orgId);
            resources = mongoTemplate.find(query, BehandlingResource.class, orgId);
        }

        resources.forEach(responseEvent::addData);
    }

    public void updateBehandling(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();
        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }
        BehandlingResource behandling = objectMapper.convertValue(responseEvent.getData().get(0), BehandlingResource.class);
        if (responseEvent.getOperation() == Operation.CREATE) {
            Query query = new Query().restrict(BehandlingResource.class);
            List<BehandlingResource> resources = mongoTemplate.find(query, BehandlingResource.class, orgId);

            Optional<BehandlingResource> any =
                    resources.stream()
                            .filter(behandlingResource -> behandlingResource.getSystemId().equals(behandling.getSystemId()))
                            .findAny();
            if (any.isPresent()) {
                throw new MongoEntryExistsException("Behandling allready exists: " + responseEvent.getOperation());
            }
            mongoTemplate.insert(behandling, orgId);
        } else if (responseEvent.getOperation() == Operation.UPDATE) {
            String identificator = responseEvent.getQuery();
            String identificatorNumber = identificator.split("/")[1];

            Query query1 = Query.query(Criteria.where("systemId.identifikatorverdi")
                    .is(identificatorNumber
                    ));
            BehandlingResource found = mongoTemplate.findOne(query1
                    , BehandlingResource.class, orgId);

            if (found != null) {
                found.setSystemId(behandling.getSystemId());
                found.setAktiv(behandling.getAktiv());
                found.setFormal(behandling.getFormal());
                found.setLinks(behandling.getLinks());
                mongoTemplate.save(found, orgId);
            } else {
                throw new MongoCantFindDocumentException(
                        "Invalid operation: systemId.identifikatorverdi "
                                + behandling.getSystemId().getIdentifikatorverdi()
                                + " doesnt exist: "
                                + responseEvent.getOperation()
                );
            }
        } else {
            throw new IllegalArgumentException("Invalid operation: " + responseEvent.getOperation());
        }
    }

    private void createSamtykker(String orgId) {
        SamtykkeResource resource = new SamtykkeResource();
        Identifikator systemId = new Identifikator();
        systemId.setIdentifikatorverdi("A");
        resource.setSystemId(systemId);
        resource.setOpprettet(Date.from(Instant.now()));
        Periode gyldighetsperiode = new Periode();
        gyldighetsperiode.setStart(Date.from(Instant.now().plus(Duration.ofDays(1))));
        resource.setGyldighetsperiode(gyldighetsperiode);
        resource.addPerson(Link.with(Person.class, "administrasjon", "personal", "person", "fodselsnummer", "01019012345"));
        resource.addOrganisasjonselement(Link.with(Organisasjonselement.class, "administrasjon", "organisasjon", "organisasjonselement", "organisasjonsid", "001122345"));
        resource.addBehandling(Link.with(Behandling.class, "systemid", "A"));
        mongoTemplate.insert(resource, orgId);
    }

    private void createTjenester(String orgId) {
        TjenesteResource resource = new TjenesteResource();
        Identifikator systemId = new Identifikator();
        systemId.setIdentifikatorverdi("A");
        resource.setSystemId(systemId);
        resource.setNavn("Intranett");
        resource.addBehandling(Link.with(Behandling.class, "systemid", "A"));
        mongoTemplate.insert(resource, orgId);
    }

    private void createBehandlinger(String orgId) {
        BehandlingResource resource = new BehandlingResource();
        Identifikator systemId = new Identifikator();
        systemId.setIdentifikatorverdi("A");
        resource.setSystemId(systemId);
        resource.setAktiv(true);
        resource.setFormal("Vise profilbilde på ansattside");
        resource.addTjeneste(Link.with(Tjeneste.class, "systemid", "A"));
        resource.addBehandlingsgrunnlag(Link.with(Behandlingsgrunnlag.class, "systemid", "A"));
        resource.addPersonopplysning(Link.with("https://beta.felleskomponent.no/fint/metamodell/attributt/bilde"));
        mongoTemplate.insert(resource, orgId);
    }
}
