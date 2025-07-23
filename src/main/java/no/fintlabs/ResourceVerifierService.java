package no.fintlabs;

import lombok.extern.slf4j.Slf4j;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.Link;
import no.fint.model.resource.personvern.samtykke.BehandlingResource;
import no.fint.model.resource.personvern.samtykke.SamtykkeResource;
import no.fint.model.resource.personvern.samtykke.TjenesteResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ResourceVerifierService {

    public boolean verifySamtykkeResource(SamtykkeResource resource) {
        return resource != null &&
                (Optional.ofNullable(resource.getGyldighetsperiode())
                        .map(Periode::getStart)
                        .isPresent()) &&
                resource.getOpprettet() != null &&
                checkIfSystemIdIsNotNull(resource.getSystemId()) &&
                hasLinks(List.of(
                        resource.getBehandling(),
                        resource.getPerson()));
    }

    public boolean verifyBehandlingResource(BehandlingResource resource) {
        return resource != null &&
                resource.getAktiv() != null &&
                StringUtils.hasText(resource.getFormal()) &&
                checkIfSystemIdIsNotNull(resource.getSystemId()) &&
                hasLinks(List.of(
                        resource.getBehandlingsgrunnlag(),
                        resource.getTjeneste(),
                        resource.getPersonopplysning()));
    }

    public boolean verifyTjenesteResource(TjenesteResource resource) {
        return resource != null &&
                StringUtils.hasText(resource.getNavn()) &&
                checkIfSystemIdIsNotNull(resource.getSystemId());
    }


    private boolean hasLinks(List<List<Link>> links) {
        if (links == null || links.isEmpty()) return false;
        for (List<Link> link : links) {
            if (link == null) return false;

            boolean hasHref = false;
            for (Link subLink : link) {
                if (subLink != null && StringUtils.hasText(subLink.getHref())) {
                    hasHref = true;
                }
            }
            if (!hasHref) return false;
        }

        return true;

    }

    private boolean checkIfSystemIdIsNotNull(Identifikator systemId) {
        return systemId != null && StringUtils.hasText(systemId.getIdentifikatorverdi());
    }

}
