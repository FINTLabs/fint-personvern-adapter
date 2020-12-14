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
import no.fint.personvern.wrapper.WrapperDocument;
import no.fint.personvern.wrapper.WrapperDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SamtykkeService {
    protected final Wrapper wrapper;
    private final WrapperDocumentRepository repository;


    private final ObjectMapper objectMapper = new ObjectMapper();

    public SamtykkeService(Wrapper wrapper, WrapperDocumentRepository repository) {
        this.wrapper = wrapper;
        this.repository = repository;
    }

    public List<SamtykkeResource> getAllSamtykke(String orgId) {
        return repository
                .findByOrgIdAndType(orgId, SamtykkeResource.class.getCanonicalName())
                .stream()
                .map(WrapperDocument::getValue)
                .map(d -> objectMapper.convertValue(d, SamtykkeResource.class))
                .collect(Collectors.toList());
    }

    public SamtykkeResource createSamtykke(Event<FintLinks> responseEvent) {
        SamtykkeResource samtykkeResource = objectMapper.convertValue(responseEvent.getData().get(0), SamtykkeResource.class);

        samtykkeResource.setSystemId(FintUtilities.createUuiSystemId());
        samtykkeResource.setOpprettet(new Date());
        repository.insert(wrapper.wrap(
                samtykkeResource,
                SamtykkeResource.class,
                responseEvent.getOrgId(),
                samtykkeResource.getSystemId().getIdentifikatorverdi()
        ));

        return samtykkeResource;
    }

    public void updateSamtykke(Event<FintLinks> responseEvent) throws MongoEntryExistsException {
        String id = responseEvent.getQuery().split("/")[1];

        Optional<WrapperDocument> byId = Optional.ofNullable(repository.findByIdAndOrgId(id, responseEvent.getOrgId()));

        repository.save(
                wrapper.update(byId.orElseThrow(MongoCantFindDocumentException::new),
                        responseEvent.getData().get(0))
        );
    }
}
