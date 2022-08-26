package no.fint.personvern.handler.samtykke.behandling;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BehandlingRepository extends JpaRepository<BehandlingEntity, String> {
}
