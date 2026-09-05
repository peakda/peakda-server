--liquibase formatted sql

--changeset peakda:20260815-001-add-bloom-categories
ALTER TABLE plants DROP CONSTRAINT ck_plants_bloom_category;
ALTER TABLE plants ADD CONSTRAINT ck_plants_bloom_category CHECK (
    bloom_category IS NULL OR bloom_category IN (
        'PLUM', 'FORSYTHIA', 'AZALEA_KR', 'CHERRY', 'CANOLA', 'AZALEA',
        'HYDRANGEA', 'LOTUS', 'SUNFLOWER', 'COSMOS', 'CHRYSANTHEMUM',
        'PINK_MUHLY', 'SILVERGRASS', 'MAPLE', 'CAMELLIA'
    )
);

ALTER TABLE attraction_blooms DROP CONSTRAINT ck_attraction_blooms_category;
ALTER TABLE attraction_blooms ADD CONSTRAINT ck_attraction_blooms_category CHECK (bloom_category IN (
    'PLUM', 'FORSYTHIA', 'AZALEA_KR', 'CHERRY', 'CANOLA', 'AZALEA',
    'HYDRANGEA', 'LOTUS', 'SUNFLOWER', 'COSMOS', 'CHRYSANTHEMUM',
    'PINK_MUHLY', 'SILVERGRASS', 'MAPLE', 'CAMELLIA'
));

ALTER TABLE seasonal_bloom_estimates DROP CONSTRAINT ck_seasonal_bloom_estimates_category;
ALTER TABLE seasonal_bloom_estimates ADD CONSTRAINT ck_seasonal_bloom_estimates_category CHECK (bloom_category IN (
    'PLUM', 'FORSYTHIA', 'AZALEA_KR', 'CHERRY', 'CANOLA', 'AZALEA',
    'HYDRANGEA', 'LOTUS', 'SUNFLOWER', 'COSMOS', 'CHRYSANTHEMUM',
    'PINK_MUHLY', 'SILVERGRASS', 'MAPLE', 'CAMELLIA'
));
--rollback ALTER TABLE plants DROP CONSTRAINT ck_plants_bloom_category;
--rollback ALTER TABLE plants ADD CONSTRAINT ck_plants_bloom_category CHECK (bloom_category IS NULL OR bloom_category IN ('PLUM', 'FORSYTHIA', 'AZALEA_KR', 'CHERRY', 'CANOLA', 'AZALEA', 'HYDRANGEA', 'LOTUS', 'COSMOS', 'PINK_MUHLY', 'SILVERGRASS', 'MAPLE', 'CAMELLIA'));
--rollback ALTER TABLE attraction_blooms DROP CONSTRAINT ck_attraction_blooms_category;
--rollback ALTER TABLE attraction_blooms ADD CONSTRAINT ck_attraction_blooms_category CHECK (bloom_category IN ('PLUM', 'FORSYTHIA', 'AZALEA_KR', 'CHERRY', 'CANOLA', 'AZALEA', 'HYDRANGEA', 'LOTUS', 'COSMOS', 'PINK_MUHLY', 'SILVERGRASS', 'MAPLE', 'CAMELLIA'));
--rollback ALTER TABLE seasonal_bloom_estimates DROP CONSTRAINT ck_seasonal_bloom_estimates_category;
--rollback ALTER TABLE seasonal_bloom_estimates ADD CONSTRAINT ck_seasonal_bloom_estimates_category CHECK (bloom_category IN ('PLUM', 'FORSYTHIA', 'AZALEA_KR', 'CHERRY', 'CANOLA', 'AZALEA', 'HYDRANGEA', 'LOTUS', 'COSMOS', 'PINK_MUHLY', 'SILVERGRASS', 'MAPLE', 'CAMELLIA'));
