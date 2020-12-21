package no.fint.personvern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.utility.FintUtilities;
import no.fint.personvern.wrapper.WrapperDocument;
import no.fint.personvern.wrapper.WrapperDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
public class BehandlingService {
    private final WrapperDocumentRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BehandlingService(WrapperDocumentRepository repository) {
        this.repository = repository;
    }

    public void getAllBehandling(Event<FintLinks> responseEvent) {
        repository
                .findByOrgIdAndType(responseEvent.getOrgId(), BehandlingResource.class.getCanonicalName())
                .stream()
                .map(WrapperDocument::getValue)
                .map(d -> objectMapper.convertValue(d, BehandlingResource.class))
                .forEach(responseEvent::addData);
    }

    public void createBehandling(Event<FintLinks> responseEvent) {
        BehandlingResource behandlingResource = objectMapper.convertValue(responseEvent.getData().get(0), BehandlingResource.class);

        behandlingResource.setSystemId(FintUtilities.createUuidSystemId());

        repository.insert(WrapperDocument.builder()
                .id(behandlingResource.getSystemId().getIdentifikatorverdi())
                .orgId(responseEvent.getOrgId())
                .value(behandlingResource)
                .type(BehandlingResource.class.getCanonicalName())
                .build());

        responseEvent.setData(Collections.singletonList(behandlingResource));
    }

    public void updateBehandling(Event<FintLinks> responseEvent) {
        String id = responseEvent.getQuery().split("/")[1];

        WrapperDocument wrapperDocument = repository.findByIdAndOrgId(id, responseEvent.getOrgId());

        if (wrapperDocument == null) {
            throw new MongoCantFindDocumentException();
        }

        BehandlingResource behandlingResource = objectMapper.convertValue(responseEvent.getData().get(0), BehandlingResource.class);

        behandlingResource.setSystemId(((BehandlingResource) wrapperDocument.getValue()).getSystemId());

        wrapperDocument.setValue(behandlingResource);

        repository.save(wrapperDocument);

        responseEvent.setData(Collections.singletonList(behandlingResource));
    }
}