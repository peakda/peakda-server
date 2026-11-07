package com.peakda.server.domain.spot.repository

import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.sql.SQLException

class SpotRecordPhotoDeletionMigrationTest {
    @Test fun `기존 스키마 upgrade는 데이터와 HIDDEN을 보존하고 active 중복과 lease 제약을 적용한다`() {
        PostgreSQLContainer("postgres:16").use { postgres ->
            postgres.start()
            val temp = Files.createTempDirectory("before-photo-deletion-")
            try {
                val files = Files.walk(Path.of("src/main/resources/db/changelog")).use { paths ->
                    paths.filter { it.toString().endsWith(".sql") && it.fileName.toString() != "004_create_spot_record_photo_deletions.sql" }
                        .sorted().map { "  - include:\n      file: " + Path.of("src/main/resources").relativize(it) }.toList()
                }
                Files.writeString(temp.resolve("before.yaml"), "databaseChangeLog:\n" + files.joinToString("\n") + "\n")
                DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
                    CompositeResourceAccessor(DirectoryResourceAccessor(temp), ClassLoaderResourceAccessor()).use { resources ->
                        Liquibase("before.yaml", resources, JdbcConnection(connection)).update(Contexts(), LabelExpression())
                        connection.autoCommit = true
                        connection.createStatement().use { s ->
                            s.executeUpdate("INSERT INTO spot_records (id,spot_id,user_id,status,created_at,updated_at) VALUES (900001,10,20,'HIDDEN',now(),now())")
                            s.executeUpdate("INSERT INTO spot_record_photo_uploads VALUES ('legacy/main.jpg',20,'thumbnail,main','SERVER_UPLOAD',true)")
                        }
                        Liquibase("db/changelog/db.changelog-master.yaml", resources, JdbcConnection(connection)).update(Contexts(), LabelExpression())
                        connection.autoCommit = true
                        connection.createStatement().use { s ->
                            s.executeQuery("SELECT status FROM spot_records WHERE id=900001").use { check(it.next()); assertThat(it.getString(1)).isEqualTo("HIDDEN") }
                            s.executeQuery("SELECT available FROM spot_record_photo_uploads WHERE object_key='legacy/main.jpg'").use { check(it.next()); assertThat(it.getBoolean(1)).isTrue() }
                            s.executeQuery("SELECT count(*) FROM spot_record_photo_deletions").use { check(it.next()); assertThat(it.getInt(1)).isZero() }
                            val insert = "INSERT INTO spot_record_photo_deletions(object_key,owner_id,evidence,variant_names,next_attempt_at) VALUES ('legacy/main.jpg',20,'SERVER_UPLOAD','thumbnail,main',now())"
                            s.executeUpdate(insert)
                            assertThatThrownBy { s.executeUpdate(insert) }.isInstanceOf(SQLException::class.java).hasMessageContaining("uq_photo_deletion_active")
                            assertThatThrownBy { s.executeUpdate("UPDATE spot_record_photo_deletions SET status='PROCESSING'") }.isInstanceOf(SQLException::class.java).hasMessageContaining("ck_photo_deletion_claim")
                            s.executeUpdate("UPDATE spot_record_photo_deletions SET status='COMPLETED',outcome='REFERENCED'")
                            s.executeUpdate(insert)
                            s.executeQuery("SELECT indexname FROM pg_indexes WHERE tablename='spot_record_photo_deletions'").use { rows ->
                                val indexes = buildList { while(rows.next()) add(rows.getString(1)) }
                                assertThat(indexes).contains("uq_photo_deletion_active", "idx_photo_deletion_due", "idx_photo_deletion_lease", "idx_photo_deletion_failed_recovery")
                            }
                        }
                        Liquibase("db/changelog/db.changelog-master.yaml", resources, JdbcConnection(connection)).update(Contexts(), LabelExpression())
                        connection.autoCommit = true
                        connection.createStatement().use { s ->
                            s.executeQuery("SELECT count(*) FROM databasechangelog WHERE id='20261108-004-create-spot-record-photo-deletions'").use { check(it.next()); assertThat(it.getInt(1)).isEqualTo(1) }
                        }
                    }
                }
            } finally { Files.deleteIfExists(temp.resolve("before.yaml")); Files.deleteIfExists(temp) }
        }
    }
}
