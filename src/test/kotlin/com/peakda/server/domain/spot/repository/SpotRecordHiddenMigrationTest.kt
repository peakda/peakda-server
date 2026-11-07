package com.peakda.server.domain.spot.repository

import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.resource.CompositeResourceAccessor
import liquibase.resource.DirectoryResourceAccessor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.sql.SQLException

class SpotRecordHiddenMigrationTest {
    @Test
    fun `기존 전체 스키마에서 새 migration만 적용하면 데이터 보존 후 HIDDEN 커밋과 복구가 가능하다`() {
        PostgreSQLContainer("postgres:16").use { postgres ->
            postgres.start()
            val temp = Files.createTempDirectory("spot-record-before-hidden-")
            try {
                // 현재 master의 월별 include 순서와 동일한, 신규 changeset 직전의 전체 스키마.
                val historicalFiles = Files.walk(Path.of("src/main/resources/db/changelog")).use { paths ->
                    paths.filter { it.toString().endsWith(".sql") && it.fileName.toString() != "003_allow_hidden_spot_records.sql" }
                        .sorted().map { "  - include:\n      file: " + Path.of("src/main/resources").relativize(it).toString() }.toList()
                }
                Files.writeString(temp.resolve("before-hidden.yaml"), "databaseChangeLog:\n" + historicalFiles.joinToString("\n") + "\n")
                DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password).use { connection ->
                    CompositeResourceAccessor(DirectoryResourceAccessor(temp), ClassLoaderResourceAccessor()).use { resources ->
                        Liquibase("before-hidden.yaml", resources, JdbcConnection(connection)).update(Contexts(), LabelExpression())
                        connection.autoCommit = true
                        connection.createStatement().use { statement ->
                            statement.executeUpdate("INSERT INTO spot_records (id,spot_id,user_id,status,published_at,created_at,updated_at) VALUES (900001,10,20,'PUBLISHED','2026-11-01T01:02:03Z',now(),now()),(900002,10,21,'DRAFT',null,now(),now())")
                            assertThatThrownBy { statement.executeUpdate("UPDATE spot_records SET status='HIDDEN' WHERE id=900001") }
                                .isInstanceOf(SQLException::class.java).hasMessageContaining("ck_spot_records_status")
                        }
                        Liquibase("db/changelog/db.changelog-master.yaml", resources, JdbcConnection(connection)).update(Contexts(), LabelExpression())
                        connection.autoCommit = true
                        connection.createStatement().use { statement ->
                            statement.executeUpdate("UPDATE spot_records SET status='HIDDEN' WHERE id=900001")
                            statement.executeQuery("SELECT status,published_at FROM spot_records WHERE id=900001").use {
                                check(it.next())
                                assertThat(it.getString(1)).isEqualTo("HIDDEN")
                                assertThat(it.getTimestamp(2).toInstant().toString()).isEqualTo("2026-11-01T01:02:03Z")
                            }
                            statement.executeUpdate("UPDATE spot_records SET status='PUBLISHED' WHERE id=900001")
                            statement.executeQuery("SELECT status FROM spot_records WHERE id=900002").use {
                                check(it.next()); assertThat(it.getString(1)).isEqualTo("DRAFT")
                            }
                            assertThatThrownBy { statement.executeUpdate("UPDATE spot_records SET status='INVALID' WHERE id=900001") }
                                .isInstanceOf(SQLException::class.java).hasMessageContaining("ck_spot_records_status")
                        }
                        // 재시작에도 이력과 CHECK가 유지되고 중복 적용되지 않는다.
                        Liquibase("db/changelog/db.changelog-master.yaml", resources, JdbcConnection(connection)).update(Contexts(), LabelExpression())
                        connection.autoCommit = true
                        connection.createStatement().use { statement ->
                            statement.executeQuery("SELECT count(*) FROM databasechangelog WHERE id='20261107-003-allow-hidden-spot-records'").use {
                                check(it.next()); assertThat(it.getInt(1)).isEqualTo(1)
                            }
                        }
                    }
                }
            } finally {
                Files.deleteIfExists(temp.resolve("before-hidden.yaml"))
                Files.deleteIfExists(temp)
            }
        }
    }
}
