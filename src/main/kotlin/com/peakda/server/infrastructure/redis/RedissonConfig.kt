package com.peakda.server.infrastructure.redis

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class RedissonConfig {

    @Bean(destroyMethod = "shutdown")
    fun redissonClient(redisProperties: RedisProperties): RedissonClient {
        val config = Config()
        val server = config.useSingleServer()
            .setAddress(redisAddress(redisProperties))
            .setDatabase(redisProperties.database)
            .setTimeout((redisProperties.timeout ?: DEFAULT_TIMEOUT).toMillis().toInt())

        redisProperties.username?.takeIf { it.isNotBlank() }?.let(server::setUsername)
        redisProperties.password?.takeIf { it.isNotBlank() }?.let(server::setPassword)

        return Redisson.create(config)
    }

    private fun redisAddress(redisProperties: RedisProperties): String {
        redisProperties.url?.takeIf { it.isNotBlank() }?.let { return it }
        val scheme = if (redisProperties.ssl.isEnabled) "rediss" else "redis"
        return "$scheme://${redisProperties.host}:${redisProperties.port}"
    }

    companion object {
        private val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(10)
    }
}
