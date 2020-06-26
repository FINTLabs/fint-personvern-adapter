package no.fint.adapter.sse;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import no.fint.adapter.FintAdapterEndpoints;
import no.fint.adapter.FintAdapterProps;
import no.fint.personvern.service.EventHandlerService;
import no.fint.event.model.HeaderConstants;
import no.fint.sse.FintSse;
import no.fint.sse.FintSseConfig;
import no.fint.sse.oauth.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles the client connections to the provider SSE endpoint
 */
@Slf4j
@Component
public class SseInitializer {

    @Getter
    private List<FintSse> sseClients = new ArrayList<>();

    @Autowired
    private FintAdapterProps props;

    @Autowired
    private FintAdapterEndpoints endpoints;

    @Autowired
    private EventHandlerService eventHandlerService;

    @Autowired(required = false)
    private TokenService tokenService;

    @PostConstruct
    @Synchronized
    public void init() {
        FintSseConfig config = FintSseConfig.withOrgIds(props.getOrganizations());
        Arrays.asList(props.getOrganizations())
                .forEach(orgId -> endpoints.getProviders()
                        .forEach((component, provider) -> {
                            FintSse fintSse = new FintSse(provider + endpoints.getSse(), tokenService, config);
                            FintEventListener fintEventListener = new FintEventListener(component, eventHandlerService);
                            fintSse.connect(fintEventListener, ImmutableMap.of(HeaderConstants.ORG_ID, orgId, HeaderConstants.CLIENT, "adapter"));
                            sseClients.add(fintSse);
                        }));
    }

    @Scheduled(initialDelay = 20000L, fixedDelay = 5000L)
    public void checkSseConnection() {
        if (sseClients.isEmpty()) {
            log.warn("Reinitializing SSE connections!");
            init();
            return;
        }
        try {
            Map<String, Long> expired = sseClients
                    .stream()
                    .collect(Collectors.toMap(FintSse::getSseUrl, FintSse::getAge, Math::max))
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue() > props.getExpiration())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (!expired.isEmpty()) {
                log.warn("Stale connections detected: {}", expired);
                cleanup();
                init();
            } else {
                for (FintSse sseClient : sseClients) {
                    if (!sseClient.verifyConnection()) {
                        log.info("Reconnecting SSE client {}", sseClient.getSseUrl());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error during SSE connection check!", e);
        }
    }

    @PreDestroy
    @Synchronized
    public void cleanup() {
        List<FintSse> oldClients = sseClients;
        sseClients = new ArrayList<>();
        for (FintSse sseClient : oldClients) {
            sseClient.close();
        }
    }
}
