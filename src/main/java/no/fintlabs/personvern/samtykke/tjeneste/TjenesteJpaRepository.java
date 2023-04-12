package no.fintlabs.personvern.samtykke.tjeneste;

import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TjenesteJpaRepository extends JpaRepository<TjenesteEntity, String> {
    List<TjenesteEntity> findByOrgId(String orgId);

    @Query("SELECT t.resource FROM Tjeneste t")
    List<TjenesteResource> findAllResources();
}
