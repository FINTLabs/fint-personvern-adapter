package no.fintlabs.personvern.samtykke.tjeneste;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import no.fintlabs.adapter.events.WriteableResourceRepository;
import no.fintlabs.adapter.models.RequestFintEvent;
import no.fintlabs.personvern.samtykke.samtykke.SamtykkeEntity;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class TjenesteRepository implements WriteableResourceRepository<TjenesteResource> {

    private final TjenesteJpaRepository tjenesteJpaRepository;

    public TjenesteRepository(TjenesteJpaRepository tjenesteJpaRepository) {
        this.tjenesteJpaRepository = tjenesteJpaRepository;
    }

    @Override
    public TjenesteResource saveResources(TjenesteResource tjenesteResource, RequestFintEvent requestFintEvent) {
        TjenesteEntity entity = TjenesteEntity.toEntity(tjenesteResource, requestFintEvent.getOrgId());
        return tjenesteJpaRepository.save(entity).getResource();
    }

    @Override
    public List<TjenesteResource> getResources() {
        return tjenesteJpaRepository.findAllResources();
    }

    @Override
    public List<TjenesteResource> getUpdatedResources() {
        return new ArrayList<>();
    }
}