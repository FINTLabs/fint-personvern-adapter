package no.fintlabs.adapter.core.config

import io.netty.channel.ChannelOption
import no.fintlabs.adapter.core.AdapterScheduler
import no.fintlabs.adapter.core.ResourceBindings
import no.fintlabs.adapter.core.TenantClient
import no.fintlabs.adapter.core.TenantRegistry
import no.fintlabs.adapter.core.auth.PasswordGrantTokenClient
import no.fintlabs.adapter.core.event.EventHandler
import no.fintlabs.adapter.core.event.EventService
import no.fintlabs.adapter.core.register.HeartbeatService
import no.fintlabs.adapter.core.register.RegistrationService
import no.fintlabs.adapter.core.sync.FullSyncService
import no.fintlabs.adapter.core.sync.ResourceSource
import no.fintlabs.adapter.core.sync.SyncPageValidator
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.resources.ConnectionProvider
import tools.jackson.databind.ObjectMapper
import java.time.Duration

/**
 * Spring Boot auto-configuration for the adapter runtime. Every bean can be replaced by defining one of the
 * same type in the application.
 */
@AutoConfiguration
@EnableConfigurationProperties(AdapterProperties::class)
class AdapterCoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun passwordGrantTokenClient(properties: AdapterProperties, webClientBuilder: WebClient.Builder): PasswordGrantTokenClient =
        PasswordGrantTokenClient(properties.tokenUri, webClientBuilder.build())

    @Bean
    @ConditionalOnMissingBean(name = ["adapterClientHttpConnector"])
    fun adapterClientHttpConnector(): ClientHttpConnector = ReactorClientHttpConnector(
        HttpClient.create(
            ConnectionProvider.builder("fint-adapter")
                .maxConnections(25)
                .pendingAcquireMaxCount(-1)
                .pendingAcquireTimeout(Duration.ofMinutes(15))
                .maxLifeTime(Duration.ofMinutes(30))
                .maxIdleTime(Duration.ofMinutes(5))
                .build(),
        )
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 900_000)
            .responseTimeout(Duration.ofMinutes(10)),
    )

    @Bean
    @ConditionalOnMissingBean
    fun tenantRegistry(
        properties: AdapterProperties,
        bindings: ResourceBindings,
        tokenClient: PasswordGrantTokenClient,
        adapterClientHttpConnector: ClientHttpConnector,
        webClientBuilder: WebClient.Builder,
    ): TenantRegistry = TenantRegistry(
        properties.enabledTenants.map { (name, tenant) ->
            TenantClient(
                name,
                tenant,
                webClientBuilder.clone()
                    .baseUrl(properties.baseUrl)
                    .clientConnector(adapterClientHttpConnector)
                    .codecs { it.defaultCodecs().maxInMemorySize(-1) }
                    .filter(tokenClient.bearerFilter(tenant))
                    .build(),
                tenant.id ?: bindings.defaultAdapterId(properties.idPrefix, tenant.orgs.first()),
            )
        },
    )

    /** Built once at startup; fails the context on inconsistent sources, handlers or capability overrides. */
    @Bean
    @ConditionalOnMissingBean
    fun resourceBindings(properties: AdapterProperties, sources: List<ResourceSource<*>>, handlers: List<EventHandler<*>>): ResourceBindings =
        ResourceBindings.of(properties, sources, handlers)

    @Bean
    @ConditionalOnMissingBean
    fun syncPageValidator() = SyncPageValidator()

    @Bean
    @ConditionalOnMissingBean
    fun fullSyncService(properties: AdapterProperties, validator: SyncPageValidator) = FullSyncService(properties, validator)

    @Bean
    @ConditionalOnMissingBean
    fun eventService(objectMapper: ObjectMapper) = EventService(objectMapper)

    @Bean
    @ConditionalOnMissingBean
    fun registrationService(properties: AdapterProperties, registry: TenantRegistry, bindings: ResourceBindings) = RegistrationService(properties, registry, bindings)

    @Bean
    @ConditionalOnMissingBean
    fun heartbeatService(registry: TenantRegistry, registrationService: RegistrationService) = HeartbeatService(registry, registrationService)

    @Bean
    @ConditionalOnMissingBean(name = ["adapterTaskScheduler"])
    fun adapterTaskScheduler(properties: AdapterProperties): ThreadPoolTaskScheduler = ThreadPoolTaskScheduler().apply {
        poolSize = properties.schedulerPoolSize
        setThreadNamePrefix("fint-adapter-")
        setWaitForTasksToCompleteOnShutdown(false)
    }

    @Bean
    @ConditionalOnMissingBean
    fun adapterScheduler(
        properties: AdapterProperties,
        registry: TenantRegistry,
        bindings: ResourceBindings,
        registrationService: RegistrationService,
        heartbeatService: HeartbeatService,
        fullSyncService: FullSyncService,
        eventService: EventService,
        adapterTaskScheduler: TaskScheduler,
    ) = AdapterScheduler(properties, registry, bindings, registrationService, heartbeatService, fullSyncService, eventService, adapterTaskScheduler)
}
