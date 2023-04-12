package no.fintlabs.personvern.samtykke.behandling;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fintlabs.adapter.events.WriteableResourceRepository;
import no.fintlabs.adapter.models.RequestFintEvent;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
public class BehandlingRepository implements WriteableResourceRepository<BehandlingResource> {

    private final BehandlingJpaRepository behandlingRepository;

    public BehandlingRepository(BehandlingJpaRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandlingResource saveResources(BehandlingResource behandlingResource, RequestFintEvent requestFintEvent) {
        BehandlingEntity entity = BehandlingEntity.toEntity(behandlingResource, requestFintEvent.getOrgId());
        return behandlingRepository.save(entity).getResource();
    }

    @Override
    public List<BehandlingResource> getResources() {
        return behandlingRepository.findAllResources();
    }

    @Override
    public List<BehandlingResource> getUpdatedResources() {
        return new ArrayList<>();
    }
}