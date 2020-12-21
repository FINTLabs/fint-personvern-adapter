package no.fint.personvern.service;

import no.fint.event.model.Event;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.felles.kompleksedatatyper.Periode;
import no.fint.model.resource.FintLinks;
import no.fint.model.resource.personvern.kodeverk.BehandlingsgrunnlagResource;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BehandlingsgrunnlagService {

    public void getAllBehandlingsgrunnlag(Event<FintLinks> responseEvent) {
        behandlingsgrunnlag.entrySet().stream()
                .map(toBehandlingsgrunnlagResource)
                .forEach(responseEvent::addData);
    }

    private final Function<Map.Entry<String, String>, BehandlingsgrunnlagResource> toBehandlingsgrunnlagResource = entry -> {
        BehandlingsgrunnlagResource behandlingsgrunnlagResource = new BehandlingsgrunnlagResource();

        Identifikator systemId = new Identifikator();
        systemId.setIdentifikatorverdi(entry.getKey());
        behandlingsgrunnlagResource.setSystemId(systemId);
        behandlingsgrunnlagResource.setKode(entry.getKey());
        behandlingsgrunnlagResource.setNavn(entry.getValue());
        behandlingsgrunnlagResource.setPassiv(false);
        Periode gyldighetsperiode = new Periode();
        gyldighetsperiode.setStart(Date.from(Instant.parse("2018-07-20T00:00:00.00Z")));
        behandlingsgrunnlagResource.setGyldighetsperiode(gyldighetsperiode);

        return behandlingsgrunnlagResource;
    };

    private final Map<String, String> behandlingsgrunnlag = Stream.of(
            new AbstractMap.SimpleImmutableEntry<>("a", "Samtykke"),
            new AbstractMap.SimpleImmutableEntry<>("b", "Nødvendig for å oppfylle en avtale"),
            new AbstractMap.SimpleImmutableEntry<>("c", "Nødvendig for å oppfylle en rettslig plikt"),
            new AbstractMap.SimpleImmutableEntry<>("d", "Nødvendig for å beskytte vitale interesser"),
            new AbstractMap.SimpleImmutableEntry<>("e", "Nødvendig for å utføre en oppgave i offentlig interesse eller utøve offentlig myndighet"),
            new AbstractMap.SimpleImmutableEntry<>("f", "Nødvendig for å ivareta legitime interesser - interesseavveiing"))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
}