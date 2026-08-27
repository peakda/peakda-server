plugins {
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.spring)
	alias(libs.plugins.kotlin.jpa)
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
}

group = "com.peakda"
version = "0.0.1-SNAPSHOT"
description = "Seasonal travel timing guide service"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.spring.boot.starter.actuator)
	implementation(libs.spring.boot.starter.aop)
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.data.redis)
	implementation(libs.spring.boot.starter.security)
	implementation(libs.spring.boot.starter.validation)
	implementation(libs.spring.boot.starter.web)
	implementation(libs.redisson)
	implementation(libs.resilience4j.spring.boot3)
	implementation(libs.jackson.module.kotlin)
	implementation(libs.jackson.dataformat.xml)
	implementation(libs.kotlin.reflect)
	implementation(libs.spring.security.oauth2.client)
	implementation(libs.springdoc.openapi.starter.webmvc.ui)
	implementation(libs.liquibase.core)
	implementation(libs.jjwt.api)
	runtimeOnly(libs.jjwt.impl)
	runtimeOnly(libs.jjwt.jackson)
	implementation(libs.aws.sdk.s3)
	implementation(libs.thumbnailator)
	implementation(libs.firebase.admin)
	annotationProcessor(libs.spring.boot.configuration.processor)
	// 코드에서 참조하지 않는다. classpath 에 있으면 Actuator 가 /actuator/prometheus 를 노출한다.
	runtimeOnly(libs.micrometer.registry.prometheus)
	runtimeOnly(libs.postgresql)
	testImplementation(libs.spring.boot.starter.test)
	testImplementation(libs.spring.boot.testcontainers)
	testImplementation(libs.kotlin.test.junit5)
	testImplementation(libs.spring.security.test)
	testImplementation(libs.testcontainers.junit.jupiter)
	testImplementation(libs.testcontainers.postgresql)
	testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
