package com.peakda.server.domain.spot.application

import com.peakda.server.common.storage.ObjectKeyUrlResolver
import org.springframework.stereotype.Component

/**
 * 기록 사진 key 를 화면 용도에 맞는 조회 URL 로 바꾼다.
 *
 * 사진은 업로드 시 여러 벌로 저장되고 DB 에는 원본(main) key 만 남는다. 나머지 variant 는
 * 같은 prefix 아래 이름만 다르므로 key 계산만으로 얻을 수 있다.
 */
@Component
class SpotRecordPhotoUrlResolver(
    private val objectKeyUrlResolver: ObjectKeyUrlResolver,
) {
    /** 원본(1600) URL. 상세 화면과 공유 카드가 쓴다. */
    fun mainUrl(mainKey: String): String = objectKeyUrlResolver.resolveKey(mainKey)

    /** 카드 썸네일(256) URL. thumbnail 은 variant 세트가 늘어나기 전 사진도 갖고 있다. */
    fun thumbnailUrl(mainKey: String): String {
        val thumbnail = SpotRecordPhotoPolicy.VARIANTS
            .firstOrNull { it.name == SpotRecordPhotoPolicy.THUMBNAIL_VARIANT }
            ?: return mainUrl(mainKey)
        return objectKeyUrlResolver.resolveKey(SpotRecordPhotoPolicy.variantKeyOf(mainKey, thumbnail))
    }

    /**
     * variant 이름 → URL 매핑.
     *
     * 아직 백필되지 않아 보유하지 않은 variant 는 원본 URL 로 폴백한다.
     * 없는 key 를 내려주면 CDN 이 404 를 캐시해 버리기 때문이다.
     */
    fun variantUrls(mainKey: String, variantNames: String?): Map<String, String> {
        val available = SpotRecordPhotoPolicy.availableVariantNames(variantNames)
        val mainUrl = mainUrl(mainKey)
        return SpotRecordPhotoPolicy.VARIANTS.associate { variant ->
            val url = if (variant.name in available) {
                objectKeyUrlResolver.resolveKey(SpotRecordPhotoPolicy.variantKeyOf(mainKey, variant))
            } else {
                mainUrl
            }
            variant.name to url
        }
    }
}
