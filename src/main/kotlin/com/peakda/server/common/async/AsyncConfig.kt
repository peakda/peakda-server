package com.peakda.server.common.async

import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.task.TaskDecorator
import org.springframework.scheduling.annotation.EnableAsync

@Configuration
@EnableAsync
class AsyncConfig {

    /**
     * `@Async` 작업에 호출한 쪽의 MDC(requestId 등)를 넘긴다. 알림 발송·수동 잡 실행 로그를
     * 그 작업을 일으킨 요청과 같은 ID 로 이어 볼 수 있다. 스프링 부트 기본 실행기가 이 빈을 적용한다.
     */
    @Bean
    fun mdcTaskDecorator(): TaskDecorator = TaskDecorator { task ->
        val context = MDC.getCopyOfContextMap()
        Runnable {
            val previous = MDC.getCopyOfContextMap()
            if (context == null) MDC.clear() else MDC.setContextMap(context)
            try {
                task.run()
            } finally {
                if (previous == null) MDC.clear() else MDC.setContextMap(previous)
            }
        }
    }
}
