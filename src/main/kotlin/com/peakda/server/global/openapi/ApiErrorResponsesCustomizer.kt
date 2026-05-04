package com.peakda.server.global.openapi

import com.peakda.server.global.model.ErrorCode
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod

@Component
class ApiErrorResponsesCustomizer : OperationCustomizer {

    override fun customize(operation: Operation, handlerMethod: HandlerMethod): Operation {
        val annotation = handlerMethod.getMethodAnnotation(ApiErrorResponses::class.java)
            ?: return operation

        val responses = operation.responses ?: ApiResponses().also { operation.responses = it }
        annotation.value.forEach { errorCode -> responses.merge(errorCode) }
        return operation
    }

    private fun ApiResponses.merge(errorCode: ErrorCode) {
        val statusCode = errorCode.httpStatus.value().toString()
        val example = Example()
            .summary(errorCode.message)
            .value(errorCode.toExampleValue())

        val existing = this[statusCode]
        if (existing == null) {
            val mediaType = MediaType().addExamples(errorCode.name, example)
            addApiResponse(
                statusCode,
                ApiResponse()
                    .description(errorCode.httpStatus.reasonPhrase)
                    .content(Content().addMediaType(MEDIA_TYPE_JSON, mediaType)),
            )
        } else {
            val content = existing.content ?: Content().also { existing.content(it) }
            val mediaType = content[MEDIA_TYPE_JSON] ?: MediaType().also { content.addMediaType(MEDIA_TYPE_JSON, it) }
            mediaType.addExamples(errorCode.name, example)
        }
    }

    private fun ErrorCode.toExampleValue(): Map<String, Any?> = linkedMapOf(
        "status" to httpStatus.value(),
        "code" to name,
        "message" to message,
    )

    companion object {
        private const val MEDIA_TYPE_JSON = "application/json"
    }
}
