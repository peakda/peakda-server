package com.peakda.server.infrastructure.push

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.util.Base64

/**
 * FCM 발송이 켜졌을 때만 Firebase 앱을 초기화한다.
 * 초기화를 발송 어댑터에서 분리해, 어댑터가 [FirebaseMessaging] 을 주입받게 한다.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.push", name = ["enabled"], havingValue = "true")
class FirebaseMessagingConfig(
    private val properties: FcmProperties,
) {

    @Bean
    fun firebaseMessaging(): FirebaseMessaging {
        val app = FirebaseApp.getApps().firstOrNull() ?: FirebaseApp.initializeApp(firebaseOptions())
        return FirebaseMessaging.getInstance(app)
    }

    private fun firebaseOptions(): FirebaseOptions {
        val credentials = properties.serviceAccountBase64
            ?.takeIf { it.isNotBlank() }
            ?.let { encoded ->
                val json = Base64.getDecoder().decode(encoded)
                ByteArrayInputStream(json).use(GoogleCredentials::fromStream)
            }
            ?: properties.serviceAccountLocation
            ?.takeIf { it.isNotBlank() }
            ?.let { FileInputStream(it).use(GoogleCredentials::fromStream) }
            ?: GoogleCredentials.getApplicationDefault()
        return FirebaseOptions.builder()
            .setCredentials(credentials)
            .apply { properties.projectId?.takeIf { it.isNotBlank() }?.let(::setProjectId) }
            .build()
    }
}
