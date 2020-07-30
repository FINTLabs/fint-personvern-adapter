package no.fint.personvern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.personvern.kodeverk.Behandlingsgrunnlag;
import no.fint.model.personvern.samtykke.Tjeneste;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.Link;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.personvern.exception.CollectionNotFoundException;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.exception.MongoEntryExistsException;
import no.fint.personvern.utility.Springer;
import no.fint.personvern.utility.SpringerRepository;
import no.fint.personvern.utility.Wrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BehandlingService extends SpringerRepository {
    @Autowired
    protected Wrapper wrapper;

    private static final String FINTLabs = "fintlabs.no";

    private final MongoTemplate mongoTemplate;
    private ObjectMapper objectMapper = new ObjectMapper();


    public BehandlingService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
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

    public void createBehandling(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();
        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }
        BehandlingResource behandlingResource = objectMapper.convertValue(responseEvent.getData().get(0), BehandlingResource.class);
        List<Springer> resources = stream(BehandlingResource.class, mongoTemplate, orgId).collect(Collectors.toList());
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
    }

    public void updateBehandling(Event<FintLinks> responseEvent) {

        String orgId = responseEvent.getOrgId();
        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }

        BehandlingResource behandlingResource = objectMapper.convertValue(responseEvent.getData().get(0), BehandlingResource.class);
        List<Springer> resources = stream(BehandlingResource.class, mongoTemplate, orgId).collect(Collectors.toList());

        String identificatorNumber = responseEvent.getQuery().split("/")[1];

        Optional<Springer> optinalOriginal =
                resources.stream()
                        .filter(fintLink -> {
                            BehandlingResource behandlingResource1 = objectMapper.convertValue(fintLink.getValue(), BehandlingResource.class);
                            return behandlingResource1.getSystemId().getIdentifikatorverdi().equals(identificatorNumber);
                        }).findFirst();
        Springer original = optinalOriginal.orElse(null);

        Optional<BehandlingResource> optinalExists =
                resources.stream()
                        .map(link -> objectMapper.convertValue(link.getValue(), BehandlingResource.class))
                        .filter(fintLink -> fintLink.getSystemId().getIdentifikatorverdi().equals(behandlingResource.getSystemId().getIdentifikatorverdi()))
                        .findFirst();
        BehandlingResource found = optinalExists.orElse(null);

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
            Springer wrap = wrapper.wrap(behandlingResource, BehandlingResource.class);
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
