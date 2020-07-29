package no.fint.personvern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
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
import no.fint.personvern.utility.Springer;
import no.fint.personvern.utility.SpringerRepository;
import no.fint.personvern.utility.Wrapper;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.stream.Collectors;

@Slf4j
@Service
public class SamtykkeService extends SpringerRepository {
    @Autowired
    protected Wrapper wrapper;

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

        query(SamtykkeResource.class, responseEvent, mongoTemplate, orgId);

        if (responseEvent.getData().isEmpty() && orgId.equals(FINTLabs)) {
            createSamtykker(orgId);
            query(SamtykkeResource.class, responseEvent, mongoTemplate, orgId);
        }
    }

    public void updateSamtykke(Event<FintLinks> responseEvent) throws MongoEntryExistsException {

        String orgId = responseEvent.getOrgId();
        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }

        SamtykkeResource samtykke = objectMapper.convertValue(responseEvent.getData().get(0), SamtykkeResource.class);
        List<Springer> resources = stream(SamtykkeResource.class, mongoTemplate, orgId).collect(Collectors.toList());

        if (responseEvent.getOperation() == Operation.CREATE) {
            Optional<SamtykkeResource> any =
                    resources.stream()
                            .map(link -> objectMapper.convertValue(link.getValue(), SamtykkeResource.class))
                            .filter(fintLink -> fintLink.getSystemId().getIdentifikatorverdi().equals(samtykke.getSystemId().getIdentifikatorverdi()))
                            .findAny();
            if (any.isPresent()) {
                ArrayList<FintLinks> fintLinks = new ArrayList<>();
                SamtykkeResource samtykkeResource = any.get();
                fintLinks.add(samtykkeResource);
                responseEvent.setData(fintLinks);
                throw new MongoEntryExistsException("Samtykke allready exists: " + responseEvent.getOperation());
            } else {
                mongoTemplate.insert(wrapper.wrap(samtykke, SamtykkeResource.class), orgId);
            }
        } else if (responseEvent.getOperation() == Operation.UPDATE) {
            String identificatorNumber = responseEvent.getQuery().split("/")[1];

            Optional<SamtykkeResource> optinalOriginal =
                    resources.stream()
                            .map(link -> objectMapper.convertValue(link.getValue(), SamtykkeResource.class))
                            .filter(fintLink -> fintLink.getSystemId().getIdentifikatorverdi().equals(identificatorNumber))
                            .findFirst();
            SamtykkeResource original = null;
            if (optinalOriginal.isPresent()){
                original = optinalOriginal.get();
            }
            Optional<SamtykkeResource> optinalExists =
                    resources.stream()
                            .map(link -> objectMapper.convertValue(link.getValue(), SamtykkeResource.class))
                            .filter(fintLink -> fintLink.getSystemId().getIdentifikatorverdi().equals(samtykke.getSystemId().getIdentifikatorverdi()))
                            .findFirst();
            SamtykkeResource found = null;
            if (optinalExists.isPresent()){
                found = optinalExists.get();
            }

            if (found !=null){
                ArrayList<FintLinks> fintLinks = new ArrayList<>();
                fintLinks.add(found);
                responseEvent.setData(fintLinks);
                throw new MongoEntryExistsException(
                        "Invalid operation: systemId.identifikatorverdi "
                                + samtykke.getSystemId().getIdentifikatorverdi()
                                + "  exist: "
                                + responseEvent.getOperation());
            }
            if (original != null) {
                original.setSystemId(samtykke.getSystemId());
                original.setGyldighetsperiode(samtykke.getGyldighetsperiode());
                original.setOpprettet(samtykke.getOpprettet());
                original.setLinks(samtykke.getLinks());
                mongoTemplate.save(wrapper.wrap(original, SamtykkeResource.class), orgId);
            } else {
                throw new MongoCantFindDocumentException(
                        "Invalid operation: systemId.identifikatorverdi "
                                + identificatorNumber
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
        TjenesteResourceWrapper tjeneste = objectMapper.convertValue(responseEvent.getData().get(0), TjenesteResourceWrapper.class);
        if (responseEvent.getOperation() == Operation.CREATE) {
            Query query = new Query().restrict(TjenesteResourceWrapper.class);
            List<TjenesteResourceWrapper> resources = mongoTemplate.find(query, TjenesteResourceWrapper.class, orgId);
            Optional<TjenesteResourceWrapper> any =
                    resources.stream()
                            .filter(tjenesteResource -> tjenesteResource.getSystemId().equals(tjeneste.getSystemId()))
                            .findAny();
            if (any.isPresent()) {
                throw new MongoEntryExistsException("Tjeneste allready exists: " + responseEvent.getOperation());
            }
            mongoTemplate.insert(tjeneste, orgId);
        } else if (responseEvent.getOperation() == Operation.UPDATE) {
            String identificatorNumber = responseEvent.getQuery().split("/")[1];

            Query query1 = Query.query(Criteria.where("systemId.identifikatorverdi")
                    .is(identificatorNumber
                    ));
            TjenesteResourceWrapper original = mongoTemplate.findOne(query1
                    , TjenesteResourceWrapper.class, orgId);

            Query query2 = Query.query(Criteria.where("systemId.identifikatorverdi")
                    .is(tjeneste.getSystemId().getIdentifikatorverdi()
                    ));
            TjenesteResourceWrapper found = mongoTemplate.findOne(query2
                    , TjenesteResourceWrapper.class, orgId);

            if (found !=null){
                ArrayList<FintLinks> fintLinks = createTjenesteResource(found);
                responseEvent.setData(fintLinks);
                throw new MongoEntryExistsException(
                        "Invalid operation: systemId.identifikatorverdi "
                                + tjeneste.getSystemId().getIdentifikatorverdi()
                                + "  exist: "
                                + responseEvent.getOperation());
            }

            if (original != null) {
                original.setSystemId(tjeneste.getSystemId());
                original.setNavn(tjeneste.getNavn());
                original.setLinks(tjeneste.getLinks());
                mongoTemplate.save(original, orgId);
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
        BehandlingResourceWrapper behandling = objectMapper.convertValue(responseEvent.getData().get(0), BehandlingResourceWrapper.class);
        if (responseEvent.getOperation() == Operation.CREATE) {
            Query query = new Query().restrict(BehandlingResource.class);
            List<BehandlingResourceWrapper> resources = mongoTemplate.find(query, BehandlingResourceWrapper.class, orgId);

            Optional<BehandlingResourceWrapper> any =
                    resources.stream()
                            .filter(behandlingResource -> behandlingResource.getSystemId().equals(behandling.getSystemId()))
                            .findAny();
            if (any.isPresent()) {
                throw new MongoEntryExistsException("Behandling allready exists: " + responseEvent.getOperation());
            }
            mongoTemplate.insert(behandling, orgId);
        } else if (responseEvent.getOperation() == Operation.UPDATE) {
            String identificatorNumber = responseEvent.getQuery().split("/")[1];

            Query query1 = Query.query(Criteria.where("systemId.identifikatorverdi")
                    .is(identificatorNumber
                    ).and("_class").is(BehandlingResourceWrapper.class.getName()));
            BehandlingResourceWrapper orginial = mongoTemplate.findOne(query1
                    , BehandlingResourceWrapper.class, orgId);

            Query query2 = Query.query(Criteria.where("systemId.identifikatorverdi")
                    .is(behandling.getSystemId().getIdentifikatorverdi()
                    ));
            BehandlingResourceWrapper found = mongoTemplate.findOne(query2
                    , BehandlingResourceWrapper.class, orgId);

            if (found !=null){
                ArrayList<FintLinks> fintLinks = createBehandlingResource(found);
                responseEvent.setData(fintLinks);
                throw new MongoEntryExistsException(
                        "Invalid operation: systemId.identifikatorverdi "
                                + behandling.getSystemId().getIdentifikatorverdi()
                                + "  exist: "
                                + responseEvent.getOperation());
            }

            if (orginial != null) {
                orginial.setSystemId(behandling.getSystemId());
                orginial.setAktiv(behandling.getAktiv());
                orginial.setFormal(behandling.getFormal());
                orginial.setLinks(behandling.getLinks());
                mongoTemplate.save(orginial, orgId);
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
    private ArrayList<FintLinks> createSamtykkeResource(SamtykkeResourceWrapper samtykkeResourceWrapper){
        ArrayList<FintLinks> fintLinks = new ArrayList<>();
        SamtykkeResource resource = new SamtykkeResource();
        resource.setGyldighetsperiode(samtykkeResourceWrapper.getGyldighetsperiode());
        resource.setLinks(samtykkeResourceWrapper.getLinks());
        resource.setOpprettet(samtykkeResourceWrapper.getOpprettet());
        resource.setSystemId(samtykkeResourceWrapper.getSystemId());
        fintLinks.add(resource);
        return fintLinks;
    }

    private ArrayList<FintLinks> createTjenesteResource(TjenesteResourceWrapper tjenesteResourceWrapper){
        ArrayList<FintLinks> fintLinks = new ArrayList<>();
        TjenesteResource resource = new TjenesteResource();
        resource.setNavn(tjenesteResourceWrapper.getNavn());
        resource.setLinks(tjenesteResourceWrapper.getLinks());
        resource.setSystemId(tjenesteResourceWrapper.getSystemId());
        fintLinks.add(resource);
        return fintLinks;
    }

    private ArrayList<FintLinks> createBehandlingResource(BehandlingResourceWrapper behandlingResourceWrapper){
        ArrayList<FintLinks> fintLinks = new ArrayList<>();
        BehandlingResource resource = new BehandlingResource();
        resource.setAktiv(behandlingResourceWrapper.getAktiv());
        resource.setLinks(behandlingResourceWrapper.getLinks());
        resource.setFormal(behandlingResourceWrapper.getFormal());
        resource.setSystemId(behandlingResourceWrapper.getSystemId());
        fintLinks.add(resource);
        return fintLinks;
    }
}
