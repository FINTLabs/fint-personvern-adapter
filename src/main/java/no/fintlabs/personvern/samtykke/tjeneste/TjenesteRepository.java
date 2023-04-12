package no.fintlabs.personvern.samtykke.tjeneste;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TjenesteRepository extends JpaRepository<TjenesteEntity, String> {
    List<TjenesteEntity> findByOrgId(String orgId);
}
