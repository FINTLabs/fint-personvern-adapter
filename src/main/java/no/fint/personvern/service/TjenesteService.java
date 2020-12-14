package no.fint.personvern.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.fint.event.model.Event;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
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
public class TjenesteService {
    private final Wrapper wrapper;
    private final WrapperDocumentRepository wrapperDocumentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public TjenesteService(Wrapper wrapper, WrapperDocumentRepository wrapperDocumentRepository) {
        this.wrapper = wrapper;
        this.wrapperDocumentRepository = wrapperDocumentRepository;
    }

    public List<TjenesteResource> getAllTjeneste(String orgId) {
        return wrapperDocumentRepository
                .findByOrgIdAndType(orgId, TjenesteResource.class.getCanonicalName())
                .stream()
                .map(WrapperDocument::getValue)
                .map(d -> objectMapper.convertValue(d, TjenesteResource.class))
                .collect(Collectors.toList());
    }

    public TjenesteResource createTjeneste(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();
        TjenesteResource tjenesteResource = objectMapper.convertValue(responseEvent.getData().get(0), TjenesteResource.class);

        tjenesteResource.setSystemId(FintUtilities.createUuiSystemId());
        wrapperDocumentRepository.insert(wrapper.wrap(
                tjenesteResource,
                TjenesteResource.class,
                orgId,
                tjenesteResource.getSystemId().getIdentifikatorverdi()
        ));

        return tjenesteResource;
    }

    public void updateTjeneste(Event<FintLinks> responseEvent) {
        String orgId = responseEvent.getOrgId();
        String id = responseEvent.getQuery().split("/")[1];

        Optional<WrapperDocument> byId = Optional.ofNullable(wrapperDocumentRepository.findByIdAndOrgId(id, orgId));

        wrapperDocumentRepository.save(
                wrapper.update(byId.orElseThrow(MongoCantFindDocumentException::new),
                        responseEvent.getData().get(0))
        );
    }
}
