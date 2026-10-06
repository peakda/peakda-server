package com.peakda.server.common.storage

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.io.ClassPathResource

/**
 * prod 프로파일의 스토리지 설정이 OCI 서버 환경 변수(render-env.sh)로 올바르게 풀리는지 확인한다.
 * 서명 리전이 버킷 리전과 다르면 OCI 가 모든 요청을 AuthorizationHeaderMalformed 로 거부한다.
 */
class StoragePropertiesProdProfileTest {

    @Test
    fun `prod storage region follows STORAGE_REGION on OCI`() {
        val properties = bindProdStorage(
            "STORAGE_BUCKET" to "peakda-prod-media",
            "STORAGE_ENDPOINT" to "https://ns.compat.objectstorage.ap-osaka-1.oraclecloud.com",
            "STORAGE_REGION" to "ap-osaka-1",
            "STORAGE_PATH_STYLE_ACCESS" to "true",
        )

        assertThat(properties.region).isEqualTo("ap-osaka-1")
        assertThat(properties.endpoint).isEqualTo("https://ns.compat.objectstorage.ap-osaka-1.oraclecloud.com")
        assertThat(properties.pathStyleAccess).isTrue()
    }

    @Test
    fun `prod storage region falls back to AWS_REGION when STORAGE_REGION is absent`() {
        val properties = bindProdStorage(
            "STORAGE_BUCKET" to "bucket",
            "AWS_REGION" to "ap-northeast-2",
        )

        assertThat(properties.region).isEqualTo("ap-northeast-2")
    }

    private fun bindProdStorage(vararg env: Pair<String, String>): StorageProperties {
        val environment = StandardEnvironment()
        val sources = environment.propertySources
        sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)
        sources.addFirst(MapPropertySource("env", mapOf(*env)))
        val loader = YamlPropertySourceLoader()
        // 나중에 로드된 프로파일 파일이 우선하도록 prod 를 기본 파일보다 앞에 둔다.
        loader.load("application-prod", ClassPathResource("application-prod.yml")).forEach(sources::addLast)
        loader.load("application", ClassPathResource("application.yml")).forEach(sources::addLast)
        return Binder.get(environment).bind("app.storage", StorageProperties::class.java).get()
    }
}
