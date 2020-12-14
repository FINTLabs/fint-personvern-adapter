package no.fint.personvern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.personvern.exception.MongoCantFindDocumentException;
import no.fint.personvern.utility.FintUtilities;
import no.fint.personvern.wrapper.Wrapper;
import no.fint.personvern.wrapper.WrapperDocument;
import no.fint.personvern.wrapper.WrapperDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BehandlingService {
    private final Wrapper wrapper;
    private final WrapperDocumentRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();


    public BehandlingService(Wrapper wrapper, WrapperDocumentRepository repository) {
        this.wrapper = wrapper;
        this.repository = repository;
    }

    public List<BehandlingResource> getAllBehandling(String orgId) {
        return repository
                .findByOrgIdAndType(orgId, BehandlingResource.class.getCanonicalName())
                .stream()
                .map(WrapperDocument::getValue)
                .map(d -> objectMapper.convertValue(d, BehandlingResource.class))
                .collect(Collectors.toList());
    }

    public BehandlingResource createBehandling(Event<FintLinks> responseEvent) {

        String orgId = responseEvent.getOrgId();
        BehandlingResource behandlingResource = objectMapper.convertValue(responseEvent.getData().get(0), BehandlingResource.class);

        behandlingResource.setSystemId(FintUtilities.createUuiSystemId());
        repository.insert(wrapper.wrap(
                behandlingResource,
                BehandlingResource.class,
                orgId,
                behandlingResource.getSystemId().getIdentifikatorverdi()
        ));

        return behandlingResource;

    }

    public void updateBehandling(Event<FintLinks> responseEvent) {
        String id = responseEvent.getQuery().split("/")[1];

        Optional<WrapperDocument> byId = Optional.ofNullable(repository.findByIdAndOrgId(id, responseEvent.getOrgId()));

        repository.save(
                wrapper.update(byId.orElseThrow(MongoCantFindDocumentException::new),
                        responseEvent.getData().get(0))
        );

    }
}
