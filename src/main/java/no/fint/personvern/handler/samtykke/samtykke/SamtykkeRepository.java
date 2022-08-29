package no.fint.personvern.handler.samtykke.samtykke;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SamtykkeRepository extends JpaRepository<SamtykkeEntity, String> {
    List<SamtykkeEntity> findByOrgId(String orgId);
}

