import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
    `java-library`
    kotlin("kapt")
}

description =
    "Tenant-aware FINT adapter runtime: registration, heartbeat, full sync and event handling driven by configuration"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xemit-jvm-type-annotations")
    }
}

dependencyManagement {
    imports {
        mavenBom(SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    api("no.fintlabs:fint-core-infra-models:3.4.0")
    api("no.novari:fint-model-resource:1.0.1")
    api("no.novari:fint-model-core:1.0.0")
    implementation("no.novari:fint-core-information-model:0.7.0")
    api("org.springframework.boot:spring-boot-starter-webclient")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    kapt("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.awaitility:awaitility")
    testImplementation(
        "no.novari:fint-personvern-resource-model-java:${
            rootProject.extra["apiVersion"] ?: project.property(
                "apiVersion",
            )
        }",
    )
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}
repositories {
    mavenCentral()
}
