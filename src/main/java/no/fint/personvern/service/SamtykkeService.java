package no.fint.personvern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.exception.MongoEntryExistsException;
import no.fint.personvern.utility.FintUtilities;
import no.fint.personvern.wrapper.WrapperDocument;
import no.fint.personvern.wrapper.WrapperDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;

@Slf4j
@Service
public class SamtykkeService {
    private final WrapperDocumentRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public SamtykkeService(WrapperDocumentRepository repository) {
        this.repository = repository;
    }

    public void getAllSamtykke(Event<FintLinks> responseEvent) {
        repository
                .findByOrgIdAndType(responseEvent.getOrgId(), SamtykkeResource.class.getCanonicalName())
                .stream()
                .map(WrapperDocument::getValue)
                .map(d -> objectMapper.convertValue(d, SamtykkeResource.class))
                .forEach(responseEvent::addData);
    }

    public void createSamtykke(Event<FintLinks> responseEvent) {
        SamtykkeResource samtykkeResource = objectMapper.convertValue(responseEvent.getData().get(0), SamtykkeResource.class);

        samtykkeResource.setSystemId(FintUtilities.createUuidSystemId());
        samtykkeResource.setOpprettet(new Date());

        repository.insert(WrapperDocument.builder()
                .id(samtykkeResource.getSystemId().getIdentifikatorverdi())
                .orgId(responseEvent.getOrgId())
                .value(samtykkeResource)
                .type(SamtykkeResource.class.getCanonicalName())
                .build());

        responseEvent.setData(Collections.singletonList(samtykkeResource));
    }

    public void updateSamtykke(Event<FintLinks> responseEvent) throws MongoEntryExistsException {
        String id = responseEvent.getQuery().split("/")[1];

        WrapperDocument wrapperDocument = repository.findByIdAndOrgId(id, responseEvent.getOrgId());

        if (wrapperDocument == null) {
            throw new MongoCantFindDocumentException();
        }

        SamtykkeResource samtykkeResource = objectMapper.convertValue(responseEvent.getData().get(0), SamtykkeResource.class);

        samtykkeResource.setSystemId(((SamtykkeResource) wrapperDocument.getValue()).getSystemId());

        wrapperDocument.setValue(samtykkeResource);

        repository.save(wrapperDocument);

        responseEvent.setData(Collections.singletonList(samtykkeResource));
    }
}