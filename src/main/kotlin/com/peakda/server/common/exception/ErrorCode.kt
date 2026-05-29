package com.peakda.server.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val httpStatus: HttpStatus,
    val message: String,
) {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),

    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 만료되었습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 유효하지 않습니다."),

    OAUTH2_PROVIDER_NOT_SUPPORTED(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인입니다."),

    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    NICKNAME_INVALID(HttpStatus.BAD_REQUEST, "닉네임은 특수문자를 제외하고 2~10자로 입력해 주세요."),
    PROFILE_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 가입이 완료된 사용자입니다."),

    EXTERNAL_API_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "외부 API를 사용할 수 없습니다."),
    EXTERNAL_API_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "외부 API 응답 시간이 초과되었습니다."),
    EXTERNAL_API_QUOTA_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "외부 API 호출 한도를 초과했습니다."),
    EXTERNAL_API_AUTH_FAILED(HttpStatus.BAD_GATEWAY, "외부 API 인증에 실패했습니다."),
    EXTERNAL_API_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "외부 API 응답 형식이 올바르지 않습니다."),
    EXTERNAL_API_BAD_REQUEST(HttpStatus.INTERNAL_SERVER_ERROR, "외부 API 요청 구성이 올바르지 않습니다."),

    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "이미지 파일이 필요합니다."),
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다."),
    IMAGE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "이미지 크기가 허용 한도를 초과했습니다."),
    IMAGE_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 처리에 실패했습니다."),
    STORAGE_UPLOAD_FAILED(HttpStatus.BAD_GATEWAY, "스토리지 업로드에 실패했습니다."),
    STORAGE_DELETE_FAILED(HttpStatus.BAD_GATEWAY, "스토리지 삭제에 실패했습니다."),

    SPOT_RECORD_PHOTO_LIMIT(HttpStatus.BAD_REQUEST, "스팟 기록 사진은 1장 이상 5장 이하로 첨부할 수 있습니다."),

    SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "스팟을 찾을 수 없습니다."),
    ATTRACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "명소를 찾을 수 없습니다."),

    SPOT_FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "찜한 스팟을 찾을 수 없습니다."),

    SPOT_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "스팟 기록을 찾을 수 없습니다."),
    SPOT_RECORD_FORBIDDEN(HttpStatus.FORBIDDEN, "본인이 작성한 스팟 기록만 수정/삭제할 수 있습니다."),
    SPOT_RECORD_INVALID_STATUS(HttpStatus.BAD_REQUEST, "게시에 필요한 필수 항목이 누락되었습니다."),

    PLANT_NOT_FOUND(HttpStatus.NOT_FOUND, "식물을 찾을 수 없습니다."),
    PLANT_INACTIVE(HttpStatus.BAD_REQUEST, "선택한 식물 중 사용할 수 없는 항목이 있습니다."),
    PLANT_SUGGESTION_DUPLICATE(HttpStatus.CONFLICT, "이미 등록되었거나 검토 중인 식물 이름입니다."),
    PLANT_SUGGESTION_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "최근 24시간 식물 제안 한도를 초과했습니다."),
}
