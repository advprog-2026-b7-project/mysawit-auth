plugins {
    java
    jacoco
    checkstyle
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "4.4.1.3373"

}

group = "id.ac.ui.cs.advprog"
version = "0.0.1-SNAPSHOT"
description = "mysawit-auth"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.postgresql:postgresql")

    implementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")

    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    implementation("com.google.api-client:google-api-client:2.2.0")
    implementation("com.google.oauth-client:google-oauth-client:1.34.1")
    implementation("com.google.http-client:google-http-client-jackson2:1.43.3")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.h2database:h2")
    testImplementation("io.rest-assured:rest-assured:5.4.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    filter {
        excludeTestsMatching("*FunctionalTest")
    }
    systemProperty("spring.datasource.url",
        "jdbc:h2:mem:mysawit_auth;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
    systemProperty("spring.datasource.username", "sa")
    systemProperty("spring.datasource.password", "")
    systemProperty("spring.datasource.driver-class-name", "org.h2.Driver")
    systemProperty("spring.jpa.hibernate.ddl-auto", "create-drop")
    systemProperty("spring.jpa.database-platform", "org.hibernate.dialect.H2Dialect")
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

checkstyle {
    toolVersion = "10.12.5"
    isIgnoreFailures = false
}

val functionalTest by tasks.registering(Test::class) {
    description = "Runs functional tests."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    shouldRunAfter(tasks.test)
    useJUnitPlatform()
    systemProperty("spring.datasource.url",
        "jdbc:h2:mem:mysawit_auth;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
    systemProperty("spring.datasource.username", "sa")
    systemProperty("spring.datasource.password", "")
    systemProperty("spring.datasource.driver-class-name", "org.h2.Driver")
    systemProperty("spring.jpa.hibernate.ddl-auto", "create-drop")
    systemProperty("spring.jpa.database-platform", "org.hibernate.dialect.H2Dialect")
    filter {
        includeTestsMatching("*FunctionalTest")
    }
    reports {
        html.outputLocation.set(
            layout.buildDirectory.dir("reports/tests/functionalTest")
        )
        junitXml.outputLocation.set(
            layout.buildDirectory.dir("test-results/functionalTest")
        )
    }
}
