package com.peakda.server.domain.feed.presentation.v2

import com.peakda.server.common.exception.GlobalExceptionHandler
import com.peakda.server.domain.feed.application.EmojiCatalog
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.http.MediaType
import org.springframework.web.accept.ContentNegotiationManager
import org.springframework.web.accept.FixedContentNegotiationStrategy
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class EmojiCatalogControllerTest {
    private val catalog = EmojiCatalog()
    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setUp() {
        mvc = MockMvcBuilders.standaloneSetup(EmojiCatalogController(catalog))
            .setContentNegotiationManager(ContentNegotiationManager(FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON)))
            .setControllerAdvice(GlobalExceptionHandler())
            .build()
    }

    @Test
    fun `catalog exposes all fully qualified standard emoji and a list ETag`() {
        mvc.perform(get("/api/v2/reaction-emojis"))
            .andExpect(status().isOk)
            .andExpect(header().string("ETag", catalog.etag))
            .andExpect(header().string("Cache-Control", containsString("max-age=86400")))
            .andExpect(jsonPath("$.data.version").value("17.0"))
            .andExpect(jsonPath("$.data.items.length()").value(3944))
            .andExpect(jsonPath("$.data.items[0].key").value("u:1f600"))
            .andExpect(jsonPath("$.data.items[0].emoji").value("😀"))
    }

    @Test
    fun `catalog returns not modified for matching list ETag`() {
        mvc.perform(get("/api/v2/reaction-emojis").header("If-None-Match", catalog.etag))
            .andExpect(status().isNotModified)
            .andExpect(header().string("ETag", catalog.etag))
            .andExpect(jsonPath("$.data").doesNotExist())
    }

    @Test
    fun `catalog treats weak matching validator as not modified`() {
        mvc.perform(get("/api/v2/reaction-emojis").header("If-None-Match", "W/${catalog.etag}"))
            .andExpect(status().isNotModified)
            .andExpect(header().string("ETag", catalog.etag))
    }
}
