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

            Optional<Springer> optinalOriginal =
                    resources.stream()
                            .filter(fintLink -> {
                                SamtykkeResource samtykkeResource = objectMapper.convertValue(fintLink.getValue(), SamtykkeResource.class);
                                return samtykkeResource.getSystemId().getIdentifikatorverdi().equals(identificatorNumber);
                            })
                            .findFirst();
            Springer original = null;
            if (optinalOriginal.isPresent()) {
                original = optinalOriginal.get();
            }
            Optional<SamtykkeResource> optinalExists =
                    resources.stream()
                            .map(link -> objectMapper.convertValue(link.getValue(), SamtykkeResource.class))
                            .filter(fintLink -> fintLink.getSystemId().getIdentifikatorverdi().equals(samtykke.getSystemId().getIdentifikatorverdi()))
                            .findFirst();
            SamtykkeResource found = null;
            if (optinalExists.isPresent()) {
                found = optinalExists.get();
            }

            if (found != null) {
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
                original.setValue(samtykke);
                mongoTemplate.save(original, orgId);
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

        query(TjenesteResource.class, responseEvent, mongoTemplate, orgId);

        if (responseEvent.getData().isEmpty() && orgId.equals(FINTLabs)) {
            createTjenester(orgId);
            query(TjenesteResource.class, responseEvent, mongoTemplate, orgId);
        }
    }

    public void updateTjeneste(Event<FintLinks> responseEvent) {

        String orgId = responseEvent.getOrgId();
        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }

        TjenesteResource tjenesteResource = objectMapper.convertValue(responseEvent.getData().get(0), TjenesteResource.class);
        List<Springer> resources = stream(TjenesteResource.class, mongoTemplate, orgId).collect(Collectors.toList());

        if (responseEvent.getOperation() == Operation.CREATE) {
            Optional<TjenesteResource> any =
                    resources.stream()
                            .map(link -> objectMapper.convertValue(link.getValue(), TjenesteResource.class))
                            .filter(fintLink -> fintLink.getSystemId().getIdentifikatorverdi().equals(tjenesteResource.getSystemId().getIdentifikatorverdi()))
                            .findAny();
            if (any.isPresent()) {
                ArrayList<FintLinks> fintLinks = new ArrayList<>();
                TjenesteResource resource = any.get();
                fintLinks.add(resource);
                responseEvent.setData(fintLinks);
                throw new MongoEntryExistsException("Tjeneste allready exists: " + responseEvent.getOperation());
            } else {
                mongoTemplate.insert(wrapper.wrap(tjenesteResource, TjenesteResource.class), orgId);
            }

        } else if (responseEvent.getOperation() == Operation.UPDATE) {
            String identificatorNumber = responseEvent.getQuery().split("/")[1];

            Optional<Springer> optinalOriginal =
                    resources.stream()
                            .filter(fintLink -> {
                                TjenesteResource tjenesteResource1 = objectMapper.convertValue(fintLink.getValue(), TjenesteResource.class);
                                return tjenesteResource1.getSystemId().getIdentifikatorverdi().equals(identificatorNumber);
                            }).findFirst();
            Springer original = null;
            if (optinalOriginal.isPresent()) {
                original = optinalOriginal.get();
            }
            Optional<TjenesteResource> optinalExists =
                    resources.stream()
                            .map(link -> objectMapper.convertValue(link.getValue(), TjenesteResource.class))
                            .filter(fintLink -> fintLink.getSystemId().getIdentifikatorverdi().equals(tjenesteResource.getSystemId().getIdentifikatorverdi()))
                            .findFirst();
            TjenesteResource found = null;
            if (optinalExists.isPresent()) {
                found = optinalExists.get();
            }
            if (found != null) {
                ArrayList<FintLinks> fintLinks = new ArrayList<>();
                fintLinks.add(found);
                responseEvent.setData(fintLinks);
                throw new MongoEntryExistsException(
                        "Invalid operation: systemId.identifikatorverdi "
                                + tjenesteResource.getSystemId().getIdentifikatorverdi()
                                + "  exist: "
                                + responseEvent.getOperation());
            }
            if (original != null) {
                original.setValue(tjenesteResource);
                mongoTemplate.save(original, orgId);
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

    public void getAllBehandling(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();

        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }

        query(BehandlingResource.class, responseEvent, mongoTemplate, orgId);

        if (responseEvent.getData().isEmpty() && orgId.equals(FINTLabs)) {
            createBehandlinger(orgId);
            query(BehandlingResource.class, responseEvent, mongoTemplate, orgId);
        }
    }

    public void updateBehandling(Event<FintLinks> responseEvent) {

        String orgId = responseEvent.getOrgId();
        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }

        BehandlingResource behandlingResource = objectMapper.convertValue(responseEvent.getData().get(0), BehandlingResource.class);
        List<Springer> resources = stream(BehandlingResource.class, mongoTemplate, orgId).collect(Collectors.toList());

        if (responseEvent.getOperation() == Operation.CREATE) {
            Optional<BehandlingResource> any =
                    resources.stream()
                            .map(link -> objectMapper.convertValue(link.getValue(), BehandlingResource.class))
                            .filter(fintLink -> fintLink.getSystemId().getIdentifikatorverdi().equals(behandlingResource.getSystemId().getIdentifikatorverdi()))
                            .findAny();
            if (any.isPresent()) {
                ArrayList<FintLinks> fintLinks = new ArrayList<>();
                BehandlingResource resource = any.get();
                fintLinks.add(resource);
                responseEvent.setData(fintLinks);
                throw new MongoEntryExistsException("Behandling allready exists: " + responseEvent.getOperation());
            } else {
                mongoTemplate.insert(wrapper.wrap(behandlingResource, BehandlingResource.class), orgId);
            }
        } else if (responseEvent.getOperation() == Operation.UPDATE) {
            String identificatorNumber = responseEvent.getQuery().split("/")[1];

            Optional<Springer> optinalOriginal =
                    resources.stream()
                            .filter(fintLink -> {
                                BehandlingResource behandlingResource1 = objectMapper.convertValue(fintLink.getValue(), BehandlingResource.class);
                                return behandlingResource1.getSystemId().getIdentifikatorverdi().equals(identificatorNumber);
                            }).findFirst();
            Springer original = null;
            if (optinalOriginal.isPresent()) {
                original = optinalOriginal.get();
            }
            Optional<BehandlingResource> optinalExists =
                    resources.stream()
                            .map(link -> objectMapper.convertValue(link.getValue(), BehandlingResource.class))
                            .filter(fintLink -> fintLink.getSystemId().getIdentifikatorverdi().equals(behandlingResource.getSystemId().getIdentifikatorverdi()))
                            .findFirst();
            BehandlingResource found = null;
            if (optinalExists.isPresent()) {
                found = optinalExists.get();
            }
            if (found != null) {
                ArrayList<FintLinks> fintLinks = new ArrayList<>();
                fintLinks.add(found);
                responseEvent.setData(fintLinks);
                throw new MongoEntryExistsException(
                        "Invalid operation: systemId.identifikatorverdi "
                                + behandlingResource.getSystemId().getIdentifikatorverdi()
                                + "  exist: "
                                + responseEvent.getOperation());
            }
            if (original != null) {
                original.setValue(behandlingResource);
                mongoTemplate.save(original, orgId);
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

    private ArrayList<FintLinks> createSamtykkeResource(SamtykkeResourceWrapper samtykkeResourceWrapper) {
        ArrayList<FintLinks> fintLinks = new ArrayList<>();
        SamtykkeResource resource = new SamtykkeResource();
        resource.setGyldighetsperiode(samtykkeResourceWrapper.getGyldighetsperiode());
        resource.setLinks(samtykkeResourceWrapper.getLinks());
        resource.setOpprettet(samtykkeResourceWrapper.getOpprettet());
        resource.setSystemId(samtykkeResourceWrapper.getSystemId());
        fintLinks.add(resource);
        return fintLinks;
    }

    private ArrayList<FintLinks> createTjenesteResource(TjenesteResourceWrapper tjenesteResourceWrapper) {
        ArrayList<FintLinks> fintLinks = new ArrayList<>();
        TjenesteResource resource = new TjenesteResource();
        resource.setNavn(tjenesteResourceWrapper.getNavn());
        resource.setLinks(tjenesteResourceWrapper.getLinks());
        resource.setSystemId(tjenesteResourceWrapper.getSystemId());
        fintLinks.add(resource);
        return fintLinks;
    }

    private ArrayList<FintLinks> createBehandlingResource(BehandlingResourceWrapper behandlingResourceWrapper) {
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
