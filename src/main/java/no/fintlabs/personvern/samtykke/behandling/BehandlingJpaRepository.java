package no.fintlabs.personvern.samtykke.behandling;

import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BehandlingJpaRepository extends JpaRepository<BehandlingEntity, String> {

    @Query("SELECT b.resource FROM Behandling b")
    List<BehandlingResource> findAllResources();

}
