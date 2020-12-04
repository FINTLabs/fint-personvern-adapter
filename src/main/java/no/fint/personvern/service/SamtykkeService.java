package no.fint.personvern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.exception.MongoEntryExistsException;
import no.fint.personvern.utility.FintUtilities;
import no.fint.personvern.wrapper.Wrapper;
import no.fint.personvern.wrapper.WrapperDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SamtykkeService extends WrapperDocumentRepository {
    protected final Wrapper wrapper;
    private final MongoService mongoService;


    private final ObjectMapper objectMapper = new ObjectMapper();

    public SamtykkeService(Wrapper wrapper, MongoService mongoService) {
        super(wrapper, mongoService.getAppProps(), mongoService.getMongoTemplate());
        this.wrapper = wrapper;
        this.mongoService = mongoService;
    }

    public void getAllSamtykke(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();

        query(SamtykkeResource.class, responseEvent, orgId);
    }

    public SamtykkeResource createSamtykke(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();
        SamtykkeResource samtykkeResource = objectMapper.convertValue(responseEvent.getData().get(0), SamtykkeResource.class);

        samtykkeResource.setSystemId(FintUtilities.createUuiSystemId());
        samtykkeResource.setOpprettet(new Date());
        mongoService.insert(
                wrapper.wrap(
                        samtykkeResource,
                        SamtykkeResource.class,
                        orgId,
                        samtykkeResource.getSystemId().getIdentifikatorverdi()
                )
        );
        return samtykkeResource;
    }

    public void updateSamtykke(Event<FintLinks> responseEvent) throws MongoEntryExistsException {
        String orgId = responseEvent.getOrgId();
        String id = responseEvent.getQuery().split("/")[1];

        mongoService.save(
                wrapper.update(stream(SamtykkeResource.class, orgId).collect(Collectors.toList())
                                .stream()
                                .filter(fintLink -> objectMapper.convertValue(fintLink.getValue(), SamtykkeResource.class)
                                        .getSystemId()
                                        .getIdentifikatorverdi()
                                        .equals(id))
                                .findFirst()
                                .orElseThrow(MongoCantFindDocumentException::new),
                        responseEvent.getData().get(0))
        );
    }
}
