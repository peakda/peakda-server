package com.peakda.server.common.exception

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartFile

class GlobalExceptionHandlerTest {

    enum class Section { PEAK_NOW, NEXT_WEEK }

    data class Body(val name: String)

    @RestController
    class TestController {
        @GetMapping("/spots")
        fun spots(@RequestParam section: Section, @RequestParam(required = false) lat: Double?) = "ok"

        @GetMapping("/header")
        fun header(@RequestHeader("X-Device") device: String) = device

        @PostMapping("/body")
        fun body(@RequestBody body: Body) = body.name

        @GetMapping("/boom")
        fun boom(): String = throw IllegalStateException("boom")

        @PostMapping("/upload")
        fun upload(@RequestPart("image") image: MultipartFile) = image.originalFilename

        // 실제로는 DispatcherServlet 이 multipart 를 해석하다 던진다(핸들러를 찾기 전).
        @PostMapping("/too-large")
        fun tooLarge(): String = throw MaxUploadSizeExceededException(-1)

        // 실제로는 응답 본문을 쓰다가 클라이언트가 연결을 끊으면(Broken pipe) 서블릿 응답 래퍼가 던진다.
        @GetMapping("/disconnected")
        fun disconnected(): String = throw AsyncRequestNotUsableException("ServletOutputStream failed to flush: Broken pipe")
    }

    private val mockMvc: MockMvc = MockMvcBuilders.standaloneSetup(TestController())
        .setControllerAdvice(GlobalExceptionHandler())
        // XML 변환기(jackson-dataformat-xml)도 있어 Accept 가 없으면 XML 로 응답한다. 클라이언트처럼 JSON 을 요청한다.
        .defaultRequest<StandaloneMockMvcBuilder>(get("/").accept(MediaType.APPLICATION_JSON))
        .build()

    @Test
    fun `필수 파라미터가 없으면 400`() {
        mockMvc.perform(get("/spots"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    @Test
    fun `enum 값이 틀리면 400`() {
        mockMvc.perform(get("/spots").param("section", "POPULAR"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    @Test
    fun `숫자 파라미터 형식이 틀리면 400`() {
        mockMvc.perform(get("/spots").param("section", "PEAK_NOW").param("lat", "abc"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    @Test
    fun `필수 헤더가 없으면 400`() {
        mockMvc.perform(get("/header"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    @Test
    fun `multipart 파트가 없으면 400`() {
        mockMvc.perform(multipart("/upload").file(MockMultipartFile("other", "a.jpg", "image/jpeg", byteArrayOf(1))))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    @Test
    fun `업로드 크기를 넘으면 413`() {
        mockMvc.perform(post("/too-large"))
            .andExpect(status().isPayloadTooLarge)
            .andExpect(jsonPath("$.code").value("IMAGE_SIZE_EXCEEDED"))
    }

    @Test
    fun `본문 JSON 이 깨져 있으면 400`() {
        mockMvc.perform(post("/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
    }

    @Test
    fun `지원하지 않는 메서드는 405`() {
        mockMvc.perform(put("/spots"))
            .andExpect(status().isMethodNotAllowed)
            .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
            .andExpect(header().string("Allow", "GET"))
    }

    @Test
    fun `지원하지 않는 Content-Type 은 415`() {
        mockMvc.perform(post("/body").contentType(MediaType.TEXT_PLAIN).content("name"))
            .andExpect(status().isUnsupportedMediaType)
            .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
            .andExpect(header().exists("Accept"))
    }

    @Test
    fun `클라이언트가 연결을 끊으면 500 응답을 쓰지 않는다`() {
        mockMvc.perform(get("/disconnected"))
            .andExpect(status().isOk)
            .andExpect(content().string(""))
    }

    @Test
    fun `처리하지 못한 예외는 여전히 500`() {
        mockMvc.perform(get("/boom"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
    }
}
