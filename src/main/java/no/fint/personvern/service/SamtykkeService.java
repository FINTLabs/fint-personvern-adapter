package no.fint.personvern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.model.administrasjon.organisasjon.Organisasjonselement;
import no.fint.model.felles.Person;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.personvern.samtykke.Behandling;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.Link;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.personvern.exception.CollectionNotFoundException;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.exception.MongoEntryExistsException;
import no.fint.personvern.utility.Springer;
import no.fint.personvern.utility.SpringerRepository;
import no.fint.personvern.utility.Wrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
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

    public void createSamtykke(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();
        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }

        SamtykkeResource samtykke = objectMapper.convertValue(responseEvent.getData().get(0), SamtykkeResource.class);
        List<Springer> resources = stream(SamtykkeResource.class, mongoTemplate, orgId).collect(Collectors.toList());

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
    }

    public void updateSamtykke(Event<FintLinks> responseEvent) throws MongoEntryExistsException {
        String orgId = responseEvent.getOrgId();
        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }

        SamtykkeResource samtykke = objectMapper.convertValue(responseEvent.getData().get(0), SamtykkeResource.class);
        List<Springer> resources = stream(SamtykkeResource.class, mongoTemplate, orgId).collect(Collectors.toList());

        String identificatorNumber = responseEvent.getQuery().split("/")[1];

        Optional<Springer> optinalOriginal =
                resources.stream()
                        .filter(fintLink -> {
                            SamtykkeResource samtykkeResource = objectMapper.convertValue(fintLink.getValue(), SamtykkeResource.class);
                            return samtykkeResource.getSystemId().getIdentifikatorverdi().equals(identificatorNumber);
                        })
                        .findFirst();
        Springer original = optinalOriginal.orElse(null);

        Optional<SamtykkeResource> optinalExists =
                resources.stream()
                        .map(link -> objectMapper.convertValue(link.getValue(), SamtykkeResource.class))
                        .filter(fintLink -> fintLink.getSystemId().getIdentifikatorverdi().equals(samtykke.getSystemId().getIdentifikatorverdi()))
                        .findFirst();
        SamtykkeResource found = optinalExists.orElse(null);

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
            Springer wrap = wrapper.wrap(samtykke, SamtykkeResource.class);
            wrap.setId(original.getId());
            wrap.setType(original.getType());
            mongoTemplate.save(wrap, orgId);
        } else {
            throw new MongoCantFindDocumentException(
                    "Invalid operation: systemId.identifikatorverdi "
                            + identificatorNumber
                            + " doesnt exist: "
                            + responseEvent.getOperation());
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
        resource.addBehandlingsgrunnlag(Link.with(Behandlingsgrunnlag.class, "personvern", "kodeverk", "behandlingsgrunnlag", "systemid", "A"));
        resource.addPersonopplysning(Link.with("https://beta.felleskomponent.no/fint/metamodell/attributt/bilde"));
        mongoTemplate.insert(resource, orgId);
    }
}
