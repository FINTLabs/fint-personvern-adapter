package no.fintlabs.personvern.samtykke.behandling;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BehandlingRepository extends JpaRepository<BehandlingEntity, String> {
    List<BehandlingEntity> findByOrgId(String orgId);
}
