package com.peakda.server.domain.spot.presentation

import com.peakda.server.common.exception.GlobalExceptionHandler
import com.peakda.server.common.exception.ErrorCode
import com.peakda.server.common.storage.StorageException
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.spot.application.CreateSpotRecordCommand
import com.peakda.server.domain.spot.application.SpotRecordPhotoUploader
import com.peakda.server.domain.spot.application.SpotRecordService
import com.peakda.server.domain.spot.application.UpdateSpotRecordCommand
import com.peakda.server.domain.spot.exception.SpotRecordForbiddenException
import com.peakda.server.domain.user.entity.User
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class SpotRecordPhotoApiContractTest {
    private val uploader = mock(SpotRecordPhotoUploader::class.java)
    private val records = mock(SpotRecordService::class.java)
    private val principal = mock(PrincipalDetails::class.java)
    private lateinit var mvc: MockMvc
    private val key = "spot-records/7/2026/10/1d09271c-0933-4dbd-a832-35d8990c8d64/main.jpg"

    @BeforeEach
    fun setUp() {
        val user = mock(User::class.java)
        `when`(user.id).thenReturn(7L)
        `when`(principal.getUser()).thenReturn(user)
        mvc = MockMvcBuilders.standaloneSetup(SpotRecordController(uploader, records))
            .setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(object : HandlerMethodArgumentResolver {
                override fun supportsParameter(parameter: MethodParameter) =
                    parameter.parameterType == PrincipalDetails::class.java

                override fun resolveArgument(
                    parameter: MethodParameter,
                    container: ModelAndViewContainer?,
                    request: NativeWebRequest,
                    factory: WebDataBinderFactory?,
                ): Any = principal
            })
            .build()
    }

    @Test
    fun `사진 업로드 응답은 추가 클라이언트 필드 없이 기존 key와 previewUrl을 반환한다`() {
        val image = MockMultipartFile("images", "photo.jpg", "image/jpeg", byteArrayOf(1))
        `when`(uploader.upload(7L, listOf(image))).thenReturn(
            listOf(SpotRecordPhotoUploader.UploadedPhoto(key, "https://cdn.example/$key", "thumbnail,medium,main")),
        )

        mvc.perform(multipart("/api/spots/records/photos").file(image).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().json(
                """{"status":200,"code":"SUCCESS","message":"OK","data":{"photos":[{"objectKey":"$key","previewUrl":"https://cdn.example/$key"}]}}""",
                true,
            ))
    }

    @Test
    fun `기존 photoKeys 수정 요청의 사진 권한 거부는 기존 403 envelope를 사용한다`() {
        val unusedCommand = mock(UpdateSpotRecordCommand::class.java)
        doThrow(SpotRecordForbiddenException()).`when`(records)
            .update(any(UpdateSpotRecordCommand::class.java) ?: unusedCommand)

        mvc.perform(patch("/api/spots/records/100").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .content("""{"photoKeys":["$key"]}"""))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.code").value("SPOT_RECORD_FORBIDDEN"))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `생성 요청도 새 입력 없이 사진 권한 거부를 기존 오류 체계로 반환한다`() {
        val unusedCommand = mock(CreateSpotRecordCommand::class.java)
        doAnswer { throw SpotRecordForbiddenException() }.`when`(records)
            .create(any(CreateSpotRecordCommand::class.java) ?: unusedCommand)

        mvc.perform(post("/api/spots/records").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).content(
            """{"spotInput":{"existingSpotId":10,"type":"LOCAL","name":"남산","latitude":37.55,"longitude":126.99},"photoKeys":["$key"],"status":"DRAFT"}""",
        ))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("SPOT_RECORD_FORBIDDEN"))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `스토리지 장애는 사진 없음의 403 대신 기존 502 오류 envelope로 반환한다`() {
        val unusedCommand = mock(UpdateSpotRecordCommand::class.java)
        doThrow(StorageException(ErrorCode.STORAGE_DOWNLOAD_FAILED)).`when`(records)
            .update(any(UpdateSpotRecordCommand::class.java) ?: unusedCommand)

        mvc.perform(patch("/api/spots/records/100").accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
            .content("""{"photoKeys":["$key"]}"""))
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.status").value(502))
            .andExpect(jsonPath("$.code").value("STORAGE_DOWNLOAD_FAILED"))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

}
