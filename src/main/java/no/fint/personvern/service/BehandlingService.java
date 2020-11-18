package no.fint.personvern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.exception.MongoEntryExistsException;
import no.fint.personvern.utility.WrapperDocumentRepository;
import no.fint.personvern.utility.Wrapper;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
public class BehandlingService extends WrapperDocumentRepository {
    protected final Wrapper wrapper;
    private final MongoService mongoService;

    private final ObjectMapper objectMapper = new ObjectMapper();


    public BehandlingService(Wrapper wrapper, MongoService mongoService) {
        super(wrapper, mongoService.getAppProps(), mongoService.getMongoTemplate());
        this.wrapper = wrapper;
        this.mongoService = mongoService;
    }

    public void getAllBehandling(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();
        query(BehandlingResource.class, responseEvent, orgId);

    }

    public void createBehandling(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();
        BehandlingResource behandlingResource = objectMapper.convertValue(responseEvent.getData().get(0), BehandlingResource.class);

        stream(BehandlingResource.class, orgId).collect(Collectors.toList())
                .stream()
                .filter(fintLink -> objectMapper.convertValue(fintLink.getValue(), BehandlingResource.class)
                        .getSystemId()
                        .getIdentifikatorverdi()
                        .equals(behandlingResource.getSystemId().getIdentifikatorverdi()))
                .findAny()
                .ifPresent(tr -> {
                    throw new MongoEntryExistsException();
                });

        mongoService.insert(wrapper.wrap(behandlingResource, BehandlingResource.class, orgId));
    }

    public void updateBehandling(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();
        String id = responseEvent.getQuery().split("/")[1];

        mongoService.save(
                wrapper.update(stream(BehandlingResource.class, orgId).collect(Collectors.toList())
                                .stream()
                                .filter(fintLink -> objectMapper.convertValue(fintLink.getValue(), BehandlingResource.class)
                                        .getSystemId()
                                        .getIdentifikatorverdi()
                                        .equals(id))
                                .findFirst()
                                .orElseThrow(MongoCantFindDocumentException::new),
                        responseEvent.getData().get(0))
        );

    }
}
