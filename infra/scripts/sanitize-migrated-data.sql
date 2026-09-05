-- Run by migrate-dev-to-prod.sh after the source dump has been restored.
-- This file is intentionally transactional: dry-run callers ROLLBACK it.
\set ON_ERROR_STOP on

CREATE TEMP TABLE migration_named_test_spots ON COMMIT DROP AS
SELECT id, attraction_id
FROM spots
WHERE name ILIKE '%테스트%';

CREATE TEMP TABLE migration_test_attractions ON COMMIT DROP AS
SELECT id, tour_api_content_id
FROM attractions
WHERE title ILIKE '%테스트%'
   OR id IN (
       SELECT attraction_id
       FROM migration_named_test_spots
       WHERE attraction_id IS NOT NULL
   );

CREATE TEMP TABLE migration_test_spots ON COMMIT DROP AS
SELECT id
FROM spots
WHERE name ILIKE '%테스트%'
   OR attraction_id IN (SELECT id FROM migration_test_attractions);

CREATE TEMP TABLE migration_test_records ON COMMIT DROP AS
SELECT id
FROM spot_records
WHERE spot_id IN (SELECT id FROM migration_test_spots);

-- Keep editorial copy even when its optional link points at a removed spot.
-- The chapter/recommendation remains useful as a place-name card without the
-- detail-link enrichment.

\echo 'Sanitization candidates'
SELECT 'attractions' AS table_name, count(*) AS rows FROM migration_test_attractions
UNION ALL SELECT 'spots', count(*) FROM migration_test_spots
UNION ALL SELECT 'spot_records', count(*) FROM migration_test_records
UNION ALL SELECT 'spot_record_photos', count(*) FROM spot_record_photos WHERE spot_record_id IN (SELECT id FROM migration_test_records)
UNION ALL SELECT 'spot_record_plants', count(*) FROM spot_record_plants WHERE spot_record_id IN (SELECT id FROM migration_test_records)
UNION ALL SELECT 'spot_record_reactions', count(*) FROM spot_record_reactions WHERE spot_record_id IN (SELECT id FROM migration_test_records)
UNION ALL SELECT 'spot_favorites', count(*) FROM spot_favorites WHERE spot_id IN (SELECT id FROM migration_test_spots)
UNION ALL SELECT 'bloom_timing_alerts', count(*) FROM bloom_timing_alerts WHERE spot_id IN (SELECT id FROM migration_test_spots)
UNION ALL SELECT 'curation_chapters', count(*) FROM curation_chapters WHERE spot_id IN (SELECT id FROM migration_test_spots)
UNION ALL SELECT 'curation_recommendations', count(*) FROM curation_recommendations WHERE spot_id IN (SELECT id FROM migration_test_spots)
UNION ALL SELECT 'attraction_blooms', count(*) FROM attraction_blooms WHERE attraction_id IN (SELECT id FROM migration_test_attractions)
UNION ALL SELECT 'seasonal_bloom_estimates', count(*) FROM seasonal_bloom_estimates WHERE attraction_id IN (SELECT id FROM migration_test_attractions)
UNION ALL SELECT 'attraction_weather_stations', count(*) FROM attraction_weather_stations WHERE attraction_id IN (SELECT id FROM migration_test_attractions)
UNION ALL SELECT 'gallery_photos', count(*) FROM gallery_photos WHERE tour_api_content_id IN (SELECT tour_api_content_id FROM migration_test_attractions);

-- Reports only support SPOT_RECORD today. Notifications are removed only when
-- their internal target is a deleted record/spot; recipient users are retained.
DELETE FROM reports
WHERE target_type = 'SPOT_RECORD'
  AND target_id IN (SELECT id FROM migration_test_records);
DELETE FROM notifications
WHERE link_type = 'INTERNAL'
  AND (
      (type = 'REACTION' AND target_id IN (SELECT id FROM migration_test_records))
      OR (type = 'TIMING' AND target_id IN (SELECT id FROM migration_test_spots))
  );
DELETE FROM admin_audit_logs
WHERE (target_type = 'SPOT_RECORD' AND target_id IN (SELECT id FROM migration_test_records))
   OR (target_type = 'SPOT' AND target_id IN (SELECT id FROM migration_test_spots))
   OR (target_type = 'ATTRACTION' AND target_id IN (SELECT id FROM migration_test_attractions));

DELETE FROM spot_record_reactions WHERE spot_record_id IN (SELECT id FROM migration_test_records);
DELETE FROM spot_record_photos WHERE spot_record_id IN (SELECT id FROM migration_test_records);
DELETE FROM spot_record_plants WHERE spot_record_id IN (SELECT id FROM migration_test_records);
DELETE FROM spot_records WHERE id IN (SELECT id FROM migration_test_records);
DELETE FROM spot_favorites WHERE spot_id IN (SELECT id FROM migration_test_spots);
DELETE FROM bloom_timing_alerts WHERE spot_id IN (SELECT id FROM migration_test_spots);
UPDATE curation_chapters SET spot_id = NULL WHERE spot_id IN (SELECT id FROM migration_test_spots);
UPDATE curation_recommendations SET spot_id = NULL WHERE spot_id IN (SELECT id FROM migration_test_spots);
DELETE FROM attraction_blooms WHERE attraction_id IN (SELECT id FROM migration_test_attractions);
DELETE FROM seasonal_bloom_estimates WHERE attraction_id IN (SELECT id FROM migration_test_attractions);
DELETE FROM attraction_weather_stations WHERE attraction_id IN (SELECT id FROM migration_test_attractions);
DELETE FROM gallery_photos WHERE tour_api_content_id IN (SELECT tour_api_content_id FROM migration_test_attractions);
DELETE FROM spots WHERE id IN (SELECT id FROM migration_test_spots);
DELETE FROM attractions WHERE id IN (SELECT id FROM migration_test_attractions);

-- Application-level integrity checks. A non-zero result aborts the caller.
DO $$
DECLARE dangling bigint;
BEGIN
  SELECT count(*) INTO dangling FROM spots s LEFT JOIN attractions a ON a.id = s.attraction_id
    WHERE s.type = 'ATTRACTION' AND a.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling spots.attraction_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM spot_records r LEFT JOIN spots s ON s.id = r.spot_id
    WHERE s.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling spot_records.spot_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM spot_record_photos p LEFT JOIN spot_records r ON r.id = p.spot_record_id
    WHERE r.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling spot_record_photos.spot_record_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM spot_record_plants p LEFT JOIN spot_records r ON r.id = p.spot_record_id
    WHERE r.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling spot_record_plants.spot_record_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM spot_record_reactions p LEFT JOIN spot_records r ON r.id = p.spot_record_id
    WHERE r.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling spot_record_reactions.spot_record_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM attraction_blooms b LEFT JOIN attractions a ON a.id = b.attraction_id
    WHERE a.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling attraction_blooms.attraction_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM seasonal_bloom_estimates e LEFT JOIN attractions a ON a.id = e.attraction_id
    WHERE a.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling seasonal_bloom_estimates.attraction_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM attraction_weather_stations w LEFT JOIN attractions a ON a.id = w.attraction_id
    WHERE a.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling attraction_weather_stations.attraction_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM spot_favorites f LEFT JOIN spots s ON s.id = f.spot_id
    WHERE s.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling spot_favorites.spot_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM bloom_timing_alerts b LEFT JOIN spots s ON s.id = b.spot_id
    WHERE s.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling bloom_timing_alerts.spot_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM curation_chapters c LEFT JOIN curations p ON p.id = c.curation_id
    WHERE p.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling curation_chapters.curation_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM curation_recommendations c LEFT JOIN curations p ON p.id = c.curation_id
    WHERE p.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling curation_recommendations.curation_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM curation_chapters c LEFT JOIN spots s ON s.id = c.spot_id
    WHERE c.spot_id IS NOT NULL AND s.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling curation_chapters.spot_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM curation_recommendations c LEFT JOIN spots s ON s.id = c.spot_id
    WHERE c.spot_id IS NOT NULL AND s.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling curation_recommendations.spot_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM festival_editorials e LEFT JOIN festivals f ON f.id = e.festival_id
    WHERE f.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling festival_editorials.festival_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM festival_highlights h LEFT JOIN festival_editorials e ON e.id = h.festival_editorial_id
    WHERE e.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling festival_highlights.festival_editorial_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM notifications n LEFT JOIN users u ON u.id = n.recipient_id
    WHERE u.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling notifications.recipient_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM device_tokens d LEFT JOIN users u ON u.id = d.user_id
    WHERE u.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling device_tokens.user_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM reports r LEFT JOIN users u ON u.id = r.reporter_id
    WHERE u.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling reports.reporter_id: %', dangling; END IF;
  SELECT count(*) INTO dangling FROM admin_audit_logs l LEFT JOIN users u ON u.id = l.admin_id
    WHERE u.id IS NULL;
  IF dangling > 0 THEN RAISE EXCEPTION 'dangling admin_audit_logs.admin_id: %', dangling; END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM attractions WHERE title ILIKE '%테스트%') THEN
    RAISE EXCEPTION 'remaining test attractions: %', (SELECT count(*) FROM attractions WHERE title ILIKE '%테스트%');
  END IF;
  IF EXISTS (SELECT 1 FROM spots WHERE name ILIKE '%테스트%') THEN
    RAISE EXCEPTION 'remaining test spots: %', (SELECT count(*) FROM spots WHERE name ILIKE '%테스트%');
  END IF;
END $$;

-- Emit a deterministic manifest for the migration artifact and fail if any
-- application table has a dangling declared foreign key.
SELECT 'row_count' AS check_name, format('%I.%I', table_schema, table_name) AS object_name,
       (xpath('/table/row/c/text()', query_to_xml(format('SELECT count(*) AS c FROM %I.%I', table_schema, table_name), true, false, '')))[1]::text::bigint AS value
  FROM information_schema.tables
 WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
 ORDER BY 2;

DO $$
DECLARE r record; dangling bigint;
BEGIN
  -- User references are intentionally application-level (the schema keeps
  -- these FKs out of the hot path), so validate every known user-id shape.
  FOR r IN
    SELECT table_name, column_name
      FROM information_schema.columns
     WHERE table_schema = 'public'
       AND table_name <> 'users'
       AND column_name IN ('user_id', 'follower_id', 'following_id', 'blocker_id', 'blocked_id', 'admin_id', 'recipient_id', 'actor_user_id', 'created_by_user_id', 'reporter_id')
  LOOP
    EXECUTE format('SELECT count(*) FROM %I x LEFT JOIN users u ON u.id = x.%I WHERE x.%I IS NOT NULL AND u.id IS NULL', r.table_name, r.column_name, r.column_name) INTO dangling;
    IF dangling > 0 THEN RAISE EXCEPTION 'dangling user reference %.%: %', r.table_name, r.column_name, dangling; END IF;
  END LOOP;

  FOR r IN
    SELECT tc.table_schema, tc.table_name, kcu.column_name,
           ccu.table_schema AS ref_schema, ccu.table_name AS ref_table, ccu.column_name AS ref_column
      FROM information_schema.table_constraints tc
      JOIN information_schema.key_column_usage kcu ON kcu.constraint_name = tc.constraint_name AND kcu.table_schema = tc.table_schema
      JOIN information_schema.constraint_column_usage ccu ON ccu.constraint_name = tc.constraint_name AND ccu.constraint_schema = tc.table_schema
     WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'public'
  LOOP
    EXECUTE format('SELECT count(*) FROM %I.%I x LEFT JOIN %I.%I y ON y.%I = x.%I WHERE x.%I IS NOT NULL AND y.%I IS NULL',
      r.table_schema, r.table_name, r.ref_schema, r.ref_table, r.ref_column, r.column_name, r.column_name, r.ref_column) INTO dangling;
    IF dangling > 0 THEN RAISE EXCEPTION 'dangling FK %.%.% -> %.%.%: %', r.table_schema, r.table_name, r.column_name, r.ref_schema, r.ref_table, r.ref_column, dangling; END IF;
  END LOOP;
END $$;
