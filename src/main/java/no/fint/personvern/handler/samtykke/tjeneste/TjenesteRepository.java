package no.fint.personvern.handler.samtykke.tjeneste;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TjenesteRepository extends JpaRepository<TjenesteEntity, String> {
}
