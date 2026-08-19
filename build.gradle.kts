plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    id("org.jetbrains.kotlinx.kover") version "0.9.3"
    application
}

group = "nukinderuru"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.2.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.2.3")
    implementation("io.ktor:ktor-server-call-logging-jvm:3.2.3")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.2.3")
    implementation("io.ktor:ktor-server-auth-jvm:3.2.3")
    implementation("io.jsonwebtoken:jjwt-api:0.12.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.2.3")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.2.3")
    implementation("io.insert-koin:koin-ktor:4.1.0")
    implementation("io.insert-koin:koin-logger-slf4j:4.1.0")
    implementation("org.jetbrains.exposed:exposed-core:0.61.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.61.0")
    implementation("org.postgresql:postgresql:42.7.7")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.7")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.7")
    testImplementation("com.h2database:h2:2.3.232")
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.2.3")
    testImplementation(kotlin("test"))
}

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

kover {
    reports {
        verify {
            rule {
                minBound(90)
            }
        }

        total {
            filters {
                includes {
                    classes(
                        "com.school21.domain.service.JwtAuthService",
                        "com.school21.domain.service.JwtProvider",
                        "com.school21.domain.service.CurrentGameService",
                        "com.school21.domain.service.MinimaxGameService",
                        "com.school21.domain.service.DefaultUserService"
                    )
                }
            }

            verify {
                rule("domain service coverage") {
                    minBound(90)
                }
            }
        }
    }
}
