package com.peakda.server.domain.curation.application

/** 업로드한 이미지가 화면 어디에 쓰이는지. 용도마다 저장 크기가 다르다. */
enum class CurationImageUsage {
    /** 큐레이션·축제 에디토리얼 상단 히어로. 글자가 얹힌 이미지라 해상도를 최대한 유지한다. */
    HERO,

    /** 챕터·추천 카드 사진. 화면에서는 가로 430px 카드로 보인다. */
    PHOTO,
}
