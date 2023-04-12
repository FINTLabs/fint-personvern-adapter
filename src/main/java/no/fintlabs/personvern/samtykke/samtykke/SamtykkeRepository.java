package no.fintlabs.personvern.samtykke.samtykke;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.felles.Person;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.personvern.samtykke.Behandling;
import no.fint.model.personvern.samtykke.Samtykke;
import no.fint.model.resource.Link;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fintlabs.adapter.events.WriteableResourceRepository;
import no.fintlabs.adapter.models.OperationType;
import no.fintlabs.adapter.models.RequestFintEvent;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.*;

@Slf4j
@Repository
public class SamtykkeRepository implements WriteableResourceRepository<SamtykkeResource> {

    private final List<SamtykkeResource> resources = new ArrayList<>();

    private long iterationCount = 0;

    @PostConstruct
    public void init() {

        for (int i = 0; i < 50; i++) {
            resources.add(createSamtykke());
        }
        log.info("Generated {} samtykke resources", getResources().size());
    }

    @Override
    public List<SamtykkeResource> getResources() {
        return resources;
    }

    @Override
    public List<SamtykkeResource> getUpdatedResources() {
        int first = 0, max = 20;

        int count = new Random().nextInt(max) + 1;
        int start = new Random().nextInt(first, getResources().size() - count);

        List<SamtykkeResource> subList = resources.subList(start, start + count);

        //if (++iterationCount % 2 == 0) {
        if (false) {
            subList.forEach(this::setGyldighetsperiode);
            log.info("Resend " + subList.size() + " changed resources");
        } else {
            log.info("Resend " + subList.size() + " resources");
        }

        return subList;
    }

    @Override
    public SamtykkeResource saveResources(SamtykkeResource samtykkeResource, RequestFintEvent requestFintEvent) {

        if (requestFintEvent.getOperationType() == OperationType.CREATE) {
            resources.add(samtykkeResource);
        } else {
            String id = samtykkeResource.getSystemId().getIdentifikatorverdi();
            int index = indexOf(id);
            if (index < 0) throw new IllegalArgumentException("No element with id found: " + id);
            resources.set(index, samtykkeResource);
        }

        return samtykkeResource;
    }

    private int indexOf(String systemId) {
        int index = -1;
        for (int i = 0; i < resources.size(); i++) {
            if (resources.get(0).getSystemId().getIdentifikatorverdi().equals(systemId)) return index;
        }
        return index;
    }

    private SamtykkeResource createSamtykke() {
        SamtykkeResource samtykkeResource = new SamtykkeResource();
        setGyldighetsperiode(samtykkeResource);

        samtykkeResource.setOpprettet(new Date());

        Identifikator identifikator = new Identifikator();
        identifikator.setIdentifikatorverdi(UUID.randomUUID().toString());
        samtykkeResource.setSystemId(identifikator);

        samtykkeResource.addBehandling(Link.with(Behandling.class, "systemid", generateComment(2)));
        samtykkeResource.addPerson(Link.with(Person.class, "systemid", generateComment(2)));
        samtykkeResource.addSelf(Link.with(Samtykke.class, "systemid", identifikator.getIdentifikatorverdi()));

        return samtykkeResource;
    }

    private void setGyldighetsperiode(SamtykkeResource resource) {
        Periode periode = new Periode();
        periode.setStart(new Date());
        periode.setSlutt(DateUtils.addMonths(new Date(), 1));
        periode.setBeskrivelse(generateComment(10));
        resource.setGyldighetsperiode(periode);
    }

    private String generateComment(int length) {
        boolean useLetters = true;
        boolean useNumbers = false;
        return RandomStringUtils.random(length, useLetters, useNumbers);
    }
}