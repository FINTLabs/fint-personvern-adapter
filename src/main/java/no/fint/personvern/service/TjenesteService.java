package no.fint.personvern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.personvern.samtykke.Behandling;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.Link;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
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
public class TjenesteService extends SpringerRepository {
    @Autowired
    protected Wrapper wrapper;

    private static final String FINTLabs = "fintlabs.no";

    private final MongoTemplate mongoTemplate;
    private ObjectMapper objectMapper = new ObjectMapper();


    public TjenesteService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
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

    public void createTjeneste(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();
        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }

        TjenesteResource tjenesteResource = objectMapper.convertValue(responseEvent.getData().get(0), TjenesteResource.class);
        List<Springer> resources = stream(TjenesteResource.class, mongoTemplate, orgId).collect(Collectors.toList());

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
    }

    public void updateTjeneste(Event<FintLinks> responseEvent) {

        String orgId = responseEvent.getOrgId();
        if (!mongoTemplate.collectionExists(orgId)) {
            throw new CollectionNotFoundException(orgId);
        }

        TjenesteResource tjenesteResource = objectMapper.convertValue(responseEvent.getData().get(0), TjenesteResource.class);
        List<Springer> resources = stream(TjenesteResource.class, mongoTemplate, orgId).collect(Collectors.toList());

        String identificatorNumber = responseEvent.getQuery().split("/")[1];

        Optional<Springer> optinalOriginal =
                resources.stream()
                        .filter(fintLink -> {
                            TjenesteResource tjenesteResource1 = objectMapper.convertValue(fintLink.getValue(), TjenesteResource.class);
                            return tjenesteResource1.getSystemId().getIdentifikatorverdi().equals(identificatorNumber);
                        }).findFirst();
        Springer original = optinalOriginal.orElse(null);

        Optional<TjenesteResource> optinalExists =
                resources.stream()
                        .map(link -> objectMapper.convertValue(link.getValue(), TjenesteResource.class))
                        .filter(fintLink -> fintLink.getSystemId().getIdentifikatorverdi().equals(tjenesteResource.getSystemId().getIdentifikatorverdi()))
                        .findFirst();

        TjenesteResource found = optinalExists.orElse(null);
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
            Springer wrap = wrapper.wrap(tjenesteResource, TjenesteResource.class);
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

    private void createTjenester(String orgId) {
        TjenesteResource resource = new TjenesteResource();
        Identifikator systemId = new Identifikator();
        systemId.setIdentifikatorverdi("A");
        resource.setSystemId(systemId);
        resource.setNavn("Intranett");
        resource.addBehandling(Link.with(Behandling.class, "systemid", "A"));
        mongoTemplate.insert(resource, orgId);
    }
}
