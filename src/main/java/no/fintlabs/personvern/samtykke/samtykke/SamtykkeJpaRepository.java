package no.fintlabs.personvern.samtykke.samtykke;

import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SamtykkeJpaRepository extends JpaRepository<SamtykkeEntity, String> {

    @Query("SELECT s.resource FROM Samtykke s")
    List<SamtykkeResource> findAllResources();

}

