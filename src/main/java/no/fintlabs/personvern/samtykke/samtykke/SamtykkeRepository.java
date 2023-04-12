package no.fintlabs.personvern.samtykke.samtykke;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fintlabs.adapter.events.WriteableResourceRepository;
import no.fintlabs.adapter.models.RequestFintEvent;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class SamtykkeRepository implements WriteableResourceRepository<SamtykkeResource> {

    private final SamtykkeJpaRepository samtykkeJpaRepository;

    public SamtykkeRepository(SamtykkeJpaRepository samtykkeJpaRepository) {
        this.samtykkeJpaRepository = samtykkeJpaRepository;
    }

    @Override
    public SamtykkeResource saveResources(SamtykkeResource samtykkeResource, RequestFintEvent requestFintEvent) {
        SamtykkeEntity entity = SamtykkeEntity.toEntity(samtykkeResource, requestFintEvent.getOrgId());
        return samtykkeJpaRepository.save(entity).getResource();
    }

    @Override
    public List<SamtykkeResource> getResources() {
        return samtykkeJpaRepository.findAllResources();
    }

    @Override
    public List<SamtykkeResource> getUpdatedResources() {
        return new ArrayList<>();
    }
}