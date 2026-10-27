package com.peakda.server.domain.admin.presentation

import com.peakda.server.common.exception.GlobalExceptionHandler
import com.peakda.server.common.security.principal.PrincipalDetails
import com.peakda.server.domain.curation.application.CurationAdminService
import com.peakda.server.domain.curation.exception.CurationNotFoundException
import com.peakda.server.domain.curation.presentation.CurationAdminController
import com.peakda.server.domain.festival.application.FestivalEditorialAdminService
import com.peakda.server.domain.festival.exception.FestivalEditorialNotFoundException
import com.peakda.server.domain.festival.exception.FestivalEditorialNotPublishedException
import com.peakda.server.domain.festival.presentation.FestivalEditorialAdminController
import com.peakda.server.domain.user.entity.User
import com.peakda.server.domain.auth.oauth.model.OAuth2LoginType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.test.util.ReflectionTestUtils

class AdminPinControllerMockMvcTest {

    private val festivalService = mock(FestivalEditorialAdminService::class.java)
    private val curationService = mock(CurationAdminService::class.java)
    private val principal = PrincipalDetails(
        User(OAuth2LoginType.KAKAO, "admin-provider", "admin").also { ReflectionTestUtils.setField(it, "id", 42L) },
    )
    private lateinit var festivalMvc: MockMvc
    private lateinit var curationMvc: MockMvc

    @BeforeEach
    fun setUp() {
        val principalResolver: HandlerMethodArgumentResolver = object : HandlerMethodArgumentResolver {
            override fun supportsParameter(parameter: MethodParameter): Boolean =
                parameter.parameterType == PrincipalDetails::class.java

            override fun resolveArgument(
                parameter: MethodParameter,
                mavContainer: org.springframework.web.method.support.ModelAndViewContainer?,
                webRequest: NativeWebRequest,
                binderFactory: org.springframework.web.bind.support.WebDataBinderFactory?,
            ): Any = principal
        }
        festivalMvc = MockMvcBuilders.standaloneSetup(FestivalEditorialAdminController(festivalService))
            .setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(principalResolver)
            .build()
        curationMvc = MockMvcBuilders.standaloneSetup(CurationAdminController(curationService))
            .setControllerAdvice(GlobalExceptionHandler())
            .setCustomArgumentResolvers(principalResolver)
            .build()
    }

    @Test
    fun `축제 고정 API는 festival id를 서비스에 전달하고 성공 응답을 반환한다`() {
        festivalMvc.perform(
            put("/api/admin/festivals/101/pin").accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("SUCCESS"))

        verify(festivalService).pin(42L, 101L)
    }

    @Test
    fun `축제 고정 해제 API는 404 에러 코드를 응답한다`() {
        doThrow(FestivalEditorialNotFoundException()).`when`(festivalService).unpin(42L, 404L)

        festivalMvc.perform(
            delete("/api/admin/festivals/404/pin").accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("FESTIVAL_EDITORIAL_NOT_FOUND"))
    }

    @Test
    fun `초안 축제 고정 API는 400 에러 코드를 응답한다`() {
        doThrow(FestivalEditorialNotPublishedException()).`when`(festivalService).pin(42L, 7L)

        festivalMvc.perform(
            put("/api/admin/festivals/7/pin").accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("FESTIVAL_EDITORIAL_NOT_PUBLISHED"))
    }

    @Test
    fun `큐레이션 고정 API는 id를 서비스에 전달하고 성공 응답을 반환한다`() {
        curationMvc.perform(put("/api/admin/curations/201/pin").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("SUCCESS"))

        verify(curationService).pin(42L, 201L)
    }

    @Test
    fun `큐레이션 고정 해제 API는 존재하지 않는 id에 404를 응답한다`() {
        doThrow(CurationNotFoundException()).`when`(curationService).unpin(42L, 404L)

        curationMvc.perform(delete("/api/admin/curations/404/pin").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("CURATION_NOT_FOUND"))
    }

    @Test
    fun `축제 고정 해제 API는 성공 응답을 반환한다`() {
        festivalMvc.perform(delete("/api/admin/festivals/101/pin").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("SUCCESS"))

        verify(festivalService).unpin(42L, 101L)
    }

    @Test
    fun `큐레이션 고정 해제 API는 성공 응답을 반환한다`() {
        curationMvc.perform(delete("/api/admin/curations/201/pin").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("SUCCESS"))

        verify(curationService).unpin(42L, 201L)
    }

    @Test
    fun `축제 고정 API는 에디토리얼이 없으면 404를 응답한다`() {
        doThrow(FestivalEditorialNotFoundException()).`when`(festivalService).pin(42L, 404L)

        festivalMvc.perform(put("/api/admin/festivals/404/pin").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("FESTIVAL_EDITORIAL_NOT_FOUND"))
    }

    @Test
    fun `큐레이션 고정 API는 존재하지 않는 id에 404를 응답한다`() {
        doThrow(CurationNotFoundException()).`when`(curationService).pin(42L, 404L)

        curationMvc.perform(put("/api/admin/curations/404/pin").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("CURATION_NOT_FOUND"))
    }
}
