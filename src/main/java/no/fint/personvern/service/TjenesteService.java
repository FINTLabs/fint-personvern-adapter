package no.fint.personvern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.exception.MongoEntryExistsException;
import no.fint.personvern.utility.SpringerRepository;
import no.fint.personvern.utility.Wrapper;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
public class TjenesteService extends SpringerRepository {
    protected final Wrapper wrapper;
    private final MongoService mongoService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TjenesteService(Wrapper wrapper, MongoService mongoService) {
        super(wrapper, mongoService.getAppProps(), mongoService.getMongoTemplate());
        this.wrapper = wrapper;
        this.mongoService = mongoService;
    }

    public void getAllTjeneste(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();

        query(TjenesteResource.class, responseEvent, orgId);
    }

    public void createTjeneste(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();
        TjenesteResource tjenesteResource = objectMapper.convertValue(responseEvent.getData().get(0), TjenesteResource.class);

        stream(TjenesteResource.class, orgId).collect(Collectors.toList())
                .stream()
                .filter(fintLink -> objectMapper.convertValue(fintLink.getValue(), TjenesteResource.class)
                        .getSystemId()
                        .getIdentifikatorverdi()
                        .equals(tjenesteResource.getSystemId().getIdentifikatorverdi()))
                .findAny()
                .ifPresent(tr -> {
                    throw new MongoEntryExistsException();
                });

        mongoService.insert(wrapper.wrap(tjenesteResource, TjenesteResource.class, orgId));
    }

    public void updateTjeneste(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();
        String id = responseEvent.getQuery().split("/")[1];

        mongoService.save(
                wrapper.update(stream(TjenesteResource.class, orgId).collect(Collectors.toList())
                                .stream()
                                .filter(fintLink -> objectMapper.convertValue(fintLink.getValue(), TjenesteResource.class)
                                        .getSystemId()
                                        .getIdentifikatorverdi()
                                        .equals(id))
                                .findFirst()
                                .orElseThrow(MongoCantFindDocumentException::new),
                        responseEvent.getData().get(0))
        );
    }
}
