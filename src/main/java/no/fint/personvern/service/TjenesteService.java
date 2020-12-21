package no.fint.personvern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.utility.FintUtilities;
import no.fint.personvern.wrapper.WrapperDocument;
import no.fint.personvern.wrapper.WrapperDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
public class TjenesteService {
    private final WrapperDocumentRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TjenesteService(WrapperDocumentRepository repository) {
        this.repository = repository;
    }

    public void getAllTjeneste(Event<FintLinks> responseEvent) {
        repository
                .findByOrgIdAndType(responseEvent.getOrgId(), TjenesteResource.class.getCanonicalName())
                .stream()
                .map(WrapperDocument::getValue)
                .map(d -> objectMapper.convertValue(d, TjenesteResource.class))
                .forEach(responseEvent::addData);
    }

    public void createTjeneste(Event<FintLinks> responseEvent) {
        TjenesteResource tjenesteResource = objectMapper.convertValue(responseEvent.getData().get(0), TjenesteResource.class);

        tjenesteResource.setSystemId(FintUtilities.createUuidSystemId());

        repository.insert(WrapperDocument.builder()
                .id(tjenesteResource.getSystemId().getIdentifikatorverdi())
                .orgId(responseEvent.getOrgId())
                .value(tjenesteResource)
                .type(TjenesteResource.class.getCanonicalName())
                .build());

        responseEvent.setData(Collections.singletonList(tjenesteResource));
    }

    public void updateTjeneste(Event<FintLinks> responseEvent) {
        String id = responseEvent.getQuery().split("/")[1];

        WrapperDocument wrapperDocument = repository.findByIdAndOrgId(id, responseEvent.getOrgId());

        if (wrapperDocument == null) {
            throw new MongoCantFindDocumentException();
        }

        TjenesteResource tjenesteResource = objectMapper.convertValue(responseEvent.getData().get(0), TjenesteResource.class);

        tjenesteResource.setSystemId(((TjenesteResource) wrapperDocument.getValue()).getSystemId());

        wrapperDocument.setValue(tjenesteResource);

        repository.save(wrapperDocument);

        responseEvent.setData(Collections.singletonList(tjenesteResource));
    }
}