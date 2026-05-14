--liquibase formatted sql

--changeset peakda:20260514-010-comment-users
COMMENT ON TABLE users IS '서비스 가입 사용자';
COMMENT ON COLUMN users.id IS '사용자 PK';
COMMENT ON COLUMN users.provider IS 'OAuth 제공자 (예: KAKAO, GOOGLE, APPLE)';
COMMENT ON COLUMN users.provider_id IS 'OAuth 제공자가 발급한 사용자 식별자';
COMMENT ON COLUMN users.email IS '사용자 이메일 (제공자가 비공개 처리할 수 있어 NULL 허용)';
COMMENT ON COLUMN users.nickname IS '서비스 노출 닉네임 (2~10자, 전역 유일)';
COMMENT ON COLUMN users.profile_image_url IS '프로필 이미지 URL';
COMMENT ON COLUMN users.status IS '사용자 상태 (ACTIVE / WITHDRAWN 등)';
COMMENT ON COLUMN users.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN users.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE users IS NULL;

--changeset peakda:20260514-010-comment-signup-sessions
COMMENT ON TABLE signup_sessions IS '소셜 로그인 후 닉네임 입력 등 회원가입 완료 전 임시 세션';
COMMENT ON COLUMN signup_sessions.id IS '회원가입 세션 PK';
COMMENT ON COLUMN signup_sessions.token IS '클라이언트가 보유하는 회원가입 진행 토큰';
COMMENT ON COLUMN signup_sessions.provider IS 'OAuth 제공자';
COMMENT ON COLUMN signup_sessions.provider_id IS 'OAuth 제공자 사용자 식별자';
COMMENT ON COLUMN signup_sessions.email IS '제공자에서 받은 이메일 (NULL 허용)';
COMMENT ON COLUMN signup_sessions.profile_image_url IS '제공자에서 받은 프로필 이미지 URL';
COMMENT ON COLUMN signup_sessions.expires_at IS '세션 만료 시각';
COMMENT ON COLUMN signup_sessions.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN signup_sessions.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE signup_sessions IS NULL;

--changeset peakda:20260514-010-comment-scheduler-job-runs
COMMENT ON TABLE scheduler_job_runs IS '스케줄러 잡 실행 이력 (관측성 / 재시도 판단용)';
COMMENT ON COLUMN scheduler_job_runs.id IS '실행 이력 PK';
COMMENT ON COLUMN scheduler_job_runs.job_name IS '실행된 잡 이름';
COMMENT ON COLUMN scheduler_job_runs.started_at IS '잡 시작 시각';
COMMENT ON COLUMN scheduler_job_runs.finished_at IS '잡 종료 시각 (RUNNING 상태에서는 NULL)';
COMMENT ON COLUMN scheduler_job_runs.status IS '실행 상태 (RUNNING / COMPLETED / FAILED)';
COMMENT ON COLUMN scheduler_job_runs.processed_count IS '처리 완료 건수';
COMMENT ON COLUMN scheduler_job_runs.total_count IS '대상 전체 건수';
COMMENT ON COLUMN scheduler_job_runs.error_message IS '실패 시 에러 메시지';
COMMENT ON COLUMN scheduler_job_runs.error_stack IS '실패 시 스택 트레이스';
--rollback COMMENT ON TABLE scheduler_job_runs IS NULL;

--changeset peakda:20260514-010-comment-attractions
COMMENT ON TABLE attractions IS '관광지/명소 마스터 (한국관광공사 TourAPI 동기화)';
COMMENT ON COLUMN attractions.id IS '관광지 PK';
COMMENT ON COLUMN attractions.tour_api_content_id IS 'TourAPI 콘텐츠 ID (외부 식별자)';
COMMENT ON COLUMN attractions.content_type_code IS 'TourAPI 콘텐츠 타입 코드 (12: 관광지, 14: 문화시설 등)';
COMMENT ON COLUMN attractions.title IS '명소 명칭';
COMMENT ON COLUMN attractions.address_main IS '주소 (시도/시군구 + 상세)';
COMMENT ON COLUMN attractions.address_detail IS '상세 주소 (동/번지 등)';
COMMENT ON COLUMN attractions.area_code IS '광역 지역 코드';
COMMENT ON COLUMN attractions.sigungu_code IS '시군구 코드';
COMMENT ON COLUMN attractions.longitude IS '경도 (GPS-X)';
COMMENT ON COLUMN attractions.latitude IS '위도 (GPS-Y)';
COMMENT ON COLUMN attractions.primary_image_url IS '대표 이미지 URL (원본)';
COMMENT ON COLUMN attractions.thumbnail_image_url IS '대표 이미지 URL (썸네일)';
COMMENT ON COLUMN attractions.category_major IS '대분류 카테고리 코드';
COMMENT ON COLUMN attractions.category_medium IS '중분류 카테고리 코드';
COMMENT ON COLUMN attractions.category_minor IS '소분류 카테고리 코드';
COMMENT ON COLUMN attractions.external_created_at IS 'TourAPI 원본 등록 일시 (yyyyMMddHHmmss 문자열)';
COMMENT ON COLUMN attractions.external_modified_at IS 'TourAPI 원본 수정 일시 (yyyyMMddHHmmss 문자열, 증분 동기화 기준)';
COMMENT ON COLUMN attractions.visible IS '서비스 노출 여부';
COMMENT ON COLUMN attractions.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN attractions.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE attractions IS NULL;

--changeset peakda:20260514-010-comment-festivals
COMMENT ON TABLE festivals IS '전국 지역 축제 (공공데이터포털 표준 데이터셋 동기화)';
COMMENT ON COLUMN festivals.id IS '축제 PK';
COMMENT ON COLUMN festivals.name IS '축제명';
COMMENT ON COLUMN festivals.venue IS '개최 장소';
COMMENT ON COLUMN festivals.start_date IS '축제 시작일 (yyyy-MM-dd 형식 문자열)';
COMMENT ON COLUMN festivals.end_date IS '축제 종료일 (yyyy-MM-dd 형식 문자열)';
COMMENT ON COLUMN festivals.host_organization IS '주관 기관명';
COMMENT ON COLUMN festivals.organizing_institution IS '주최 기관명';
COMMENT ON COLUMN festivals.supporting_institution IS '후원 기관명';
COMMENT ON COLUMN festivals.phone_number IS '문의 전화번호';
COMMENT ON COLUMN festivals.homepage_url IS '축제 홈페이지 URL';
COMMENT ON COLUMN festivals.road_address IS '도로명 주소';
COMMENT ON COLUMN festivals.land_lot_address IS '지번 주소';
COMMENT ON COLUMN festivals.latitude IS '위도';
COMMENT ON COLUMN festivals.longitude IS '경도';
COMMENT ON COLUMN festivals.reference_date IS '데이터 기준일';
COMMENT ON COLUMN festivals.provider_institution_code IS '제공 기관 코드';
COMMENT ON COLUMN festivals.provider_institution_name IS '제공 기관명';
COMMENT ON COLUMN festivals.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN festivals.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE festivals IS NULL;

--changeset peakda:20260514-010-comment-weather-mid-forecasts
COMMENT ON TABLE weather_mid_forecasts IS '기상청 중기예보 (3~10일 후 육상/기온, 발표 시각별 1건)';
COMMENT ON COLUMN weather_mid_forecasts.id IS '중기예보 PK';
COMMENT ON COLUMN weather_mid_forecasts.region_code IS '예보 구역 코드 (기상청 중기예보 지역 ID)';
COMMENT ON COLUMN weather_mid_forecasts.announce_time IS '발표 시각 (yyyyMMddHHmm 문자열, 일 2회 06/18시)';
COMMENT ON COLUMN weather_mid_forecasts.weather_day3_am IS '3일 후 오전 날씨 예보 (맑음/흐림 등)';
COMMENT ON COLUMN weather_mid_forecasts.weather_day3_pm IS '3일 후 오후 날씨 예보';
COMMENT ON COLUMN weather_mid_forecasts.weather_day4_am IS '4일 후 오전 날씨 예보';
COMMENT ON COLUMN weather_mid_forecasts.weather_day4_pm IS '4일 후 오후 날씨 예보';
COMMENT ON COLUMN weather_mid_forecasts.weather_day5_am IS '5일 후 오전 날씨 예보';
COMMENT ON COLUMN weather_mid_forecasts.weather_day5_pm IS '5일 후 오후 날씨 예보';
COMMENT ON COLUMN weather_mid_forecasts.weather_day6_am IS '6일 후 오전 날씨 예보';
COMMENT ON COLUMN weather_mid_forecasts.weather_day6_pm IS '6일 후 오후 날씨 예보';
COMMENT ON COLUMN weather_mid_forecasts.weather_day7_am IS '7일 후 오전 날씨 예보';
COMMENT ON COLUMN weather_mid_forecasts.weather_day7_pm IS '7일 후 오후 날씨 예보';
COMMENT ON COLUMN weather_mid_forecasts.weather_day8 IS '8일 후 날씨 예보 (일 단위)';
COMMENT ON COLUMN weather_mid_forecasts.weather_day9 IS '9일 후 날씨 예보 (일 단위)';
COMMENT ON COLUMN weather_mid_forecasts.weather_day10 IS '10일 후 날씨 예보 (일 단위)';
COMMENT ON COLUMN weather_mid_forecasts.rain_probability_day3_am IS '3일 후 오전 강수확률(%)';
COMMENT ON COLUMN weather_mid_forecasts.rain_probability_day3_pm IS '3일 후 오후 강수확률(%)';
COMMENT ON COLUMN weather_mid_forecasts.rain_probability_day4_am IS '4일 후 오전 강수확률(%)';
COMMENT ON COLUMN weather_mid_forecasts.rain_probability_day4_pm IS '4일 후 오후 강수확률(%)';
COMMENT ON COLUMN weather_mid_forecasts.rain_probability_day5_am IS '5일 후 오전 강수확률(%)';
COMMENT ON COLUMN weather_mid_forecasts.rain_probability_day5_pm IS '5일 후 오후 강수확률(%)';
COMMENT ON COLUMN weather_mid_forecasts.rain_probability_day6_am IS '6일 후 오전 강수확률(%)';
COMMENT ON COLUMN weather_mid_forecasts.rain_probability_day6_pm IS '6일 후 오후 강수확률(%)';
COMMENT ON COLUMN weather_mid_forecasts.rain_probability_day7_am IS '7일 후 오전 강수확률(%)';
COMMENT ON COLUMN weather_mid_forecasts.rain_probability_day7_pm IS '7일 후 오후 강수확률(%)';
COMMENT ON COLUMN weather_mid_forecasts.rain_probability_day8 IS '8일 후 강수확률(%)';
COMMENT ON COLUMN weather_mid_forecasts.rain_probability_day9 IS '9일 후 강수확률(%)';
COMMENT ON COLUMN weather_mid_forecasts.rain_probability_day10 IS '10일 후 강수확률(%)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_min_day3 IS '3일 후 최저 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_max_day3 IS '3일 후 최고 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_min_day4 IS '4일 후 최저 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_max_day4 IS '4일 후 최고 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_min_day5 IS '5일 후 최저 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_max_day5 IS '5일 후 최고 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_min_day6 IS '6일 후 최저 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_max_day6 IS '6일 후 최고 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_min_day7 IS '7일 후 최저 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_max_day7 IS '7일 후 최고 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_min_day8 IS '8일 후 최저 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_max_day8 IS '8일 후 최고 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_min_day9 IS '9일 후 최저 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_max_day9 IS '9일 후 최고 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_min_day10 IS '10일 후 최저 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.temperature_max_day10 IS '10일 후 최고 기온(℃)';
COMMENT ON COLUMN weather_mid_forecasts.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN weather_mid_forecasts.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE weather_mid_forecasts IS NULL;

--changeset peakda:20260514-010-comment-weather-short-forecasts
COMMENT ON TABLE weather_short_forecasts IS '기상청 단기예보 (격자 단위, 발표 시각별 카테고리/예보 시각 조합)';
COMMENT ON COLUMN weather_short_forecasts.id IS '단기예보 PK';
COMMENT ON COLUMN weather_short_forecasts.grid_x IS '기상청 격자 X 좌표';
COMMENT ON COLUMN weather_short_forecasts.grid_y IS '기상청 격자 Y 좌표';
COMMENT ON COLUMN weather_short_forecasts.announce_date IS '예보 발표 일자 (yyyyMMdd 문자열)';
COMMENT ON COLUMN weather_short_forecasts.announce_time IS '예보 발표 시각 (HHmm 문자열)';
COMMENT ON COLUMN weather_short_forecasts.forecast_date IS '예보 대상 일자 (yyyyMMdd 문자열)';
COMMENT ON COLUMN weather_short_forecasts.forecast_time IS '예보 대상 시각 (HHmm 문자열)';
COMMENT ON COLUMN weather_short_forecasts.forecast_category IS '예보 항목 코드 (POP 강수확률, PTY 강수형태, SKY 하늘상태, TMP 1시간기온 등)';
COMMENT ON COLUMN weather_short_forecasts.forecast_value IS '예보 값 (카테고리별 단위/표현이 다른 문자열)';
COMMENT ON COLUMN weather_short_forecasts.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN weather_short_forecasts.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE weather_short_forecasts IS NULL;

--changeset peakda:20260514-010-comment-walking-routes
COMMENT ON TABLE walking_routes IS '두루누비 걷기 여행길 (코스 묶음 단위)';
COMMENT ON COLUMN walking_routes.id IS '걷기 여행길 PK';
COMMENT ON COLUMN walking_routes.durunubi_route_id IS '두루누비 여행길 식별자 (외부 식별자)';
COMMENT ON COLUMN walking_routes.route_name IS '여행길 명칭';
COMMENT ON COLUMN walking_routes.region_division IS '광역 구분 (시도 등)';
COMMENT ON COLUMN walking_routes.theme_name IS '테마명 (해안길, 숲길 등)';
COMMENT ON COLUMN walking_routes.city_county IS '시군 구분';
COMMENT ON COLUMN walking_routes.distance IS '총 거리 (원본 문자열, 단위 포함 가능)';
COMMENT ON COLUMN walking_routes.required_time IS '총 소요 시간 (원본 문자열)';
COMMENT ON COLUMN walking_routes.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN walking_routes.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE walking_routes IS NULL;

--changeset peakda:20260514-010-comment-walking-courses
COMMENT ON TABLE walking_courses IS '두루누비 걷기 코스 (route 하위 세부 코스)';
COMMENT ON COLUMN walking_courses.id IS '걷기 코스 PK';
COMMENT ON COLUMN walking_courses.durunubi_course_id IS '두루누비 코스 식별자 (외부 식별자)';
COMMENT ON COLUMN walking_courses.durunubi_route_id IS '소속 여행길 식별자 (walking_routes.durunubi_route_id)';
COMMENT ON COLUMN walking_courses.name IS '코스 한글 명칭';
COMMENT ON COLUMN walking_courses.distance IS '코스 거리 (원본 문자열)';
COMMENT ON COLUMN walking_courses.total_required_time IS '코스 총 소요 시간 (원본 문자열)';
COMMENT ON COLUMN walking_courses.difficulty_level IS '코스 난이도';
COMMENT ON COLUMN walking_courses.city_county IS '시군 구분';
COMMENT ON COLUMN walking_courses.region_division IS '광역 구분';
COMMENT ON COLUMN walking_courses.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN walking_courses.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE walking_courses IS NULL;

--changeset peakda:20260514-010-comment-congestions
COMMENT ON TABLE congestions IS '관광지 혼잡도 (한국관광공사 TATS 일자별)';
COMMENT ON COLUMN congestions.id IS '혼잡도 PK';
COMMENT ON COLUMN congestions.base_date IS '기준 일자 (yyyyMMdd 문자열)';
COMMENT ON COLUMN congestions.tourist_attraction_code IS '관광지 코드 (TATS 식별자)';
COMMENT ON COLUMN congestions.tourist_attraction_name IS '관광지 명칭';
COMMENT ON COLUMN congestions.area_code IS '광역 지역 코드';
COMMENT ON COLUMN congestions.sigungu_code IS '시군구 코드';
COMMENT ON COLUMN congestions.congestion_rate IS '혼잡도 지수 (원본 문자열, 단위 포함 가능)';
COMMENT ON COLUMN congestions.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN congestions.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE congestions IS NULL;

--changeset peakda:20260514-010-comment-region-visitors
COMMENT ON TABLE region_visitors IS '지역별 방문자 수 (한국관광 데이터랩, 관광객 구분별)';
COMMENT ON COLUMN region_visitors.id IS '방문자 통계 PK';
COMMENT ON COLUMN region_visitors.base_date IS '기준 일자 (yyyyMMdd 문자열)';
COMMENT ON COLUMN region_visitors.area_code IS '광역 지역 코드';
COMMENT ON COLUMN region_visitors.area_name IS '광역 지역 명칭';
COMMENT ON COLUMN region_visitors.tourist_type_code IS '관광객 구분 코드 (현지인/외지인/외국인 등)';
COMMENT ON COLUMN region_visitors.tourist_type_name IS '관광객 구분 명칭';
COMMENT ON COLUMN region_visitors.visitor_count IS '방문자 수';
COMMENT ON COLUMN region_visitors.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN region_visitors.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE region_visitors IS NULL;

--changeset peakda:20260514-010-comment-gallery-photos
COMMENT ON TABLE gallery_photos IS '관광 갤러리 사진 (한국관광공사 TourAPI 갤러리)';
COMMENT ON COLUMN gallery_photos.id IS '갤러리 사진 PK';
COMMENT ON COLUMN gallery_photos.tour_api_content_id IS 'TourAPI 갤러리 콘텐츠 ID (외부 식별자)';
COMMENT ON COLUMN gallery_photos.content_type_code IS '갤러리 콘텐츠 타입 코드';
COMMENT ON COLUMN gallery_photos.title IS '사진 제목';
COMMENT ON COLUMN gallery_photos.web_image_url IS '웹용 이미지 URL';
COMMENT ON COLUMN gallery_photos.external_created_at IS 'TourAPI 원본 등록 일시 (yyyyMMddHHmmss 문자열)';
COMMENT ON COLUMN gallery_photos.external_modified_at IS 'TourAPI 원본 수정 일시 (yyyyMMddHHmmss 문자열)';
COMMENT ON COLUMN gallery_photos.photography_month IS '촬영 월 (yyyyMM 문자열)';
COMMENT ON COLUMN gallery_photos.photography_location IS '촬영 장소';
COMMENT ON COLUMN gallery_photos.photographer IS '촬영자';
COMMENT ON COLUMN gallery_photos.search_keyword IS '검색 키워드 (쉼표 구분 가능)';
COMMENT ON COLUMN gallery_photos.created_at IS '레코드 생성 시각';
COMMENT ON COLUMN gallery_photos.updated_at IS '레코드 최종 수정 시각';
--rollback COMMENT ON TABLE gallery_photos IS NULL;
