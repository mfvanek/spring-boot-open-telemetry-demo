/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.kotlin.test

import io.github.mfvanek.pg.core.checks.common.DatabaseCheckOnHost
import io.github.mfvanek.pg.core.checks.common.Diagnostic
import io.github.mfvanek.pg.model.context.PgContext
import io.github.mfvanek.pg.model.dbobject.DbObject
import io.github.mfvanek.pg.model.predicates.SkipLiquibaseTablesPredicate
import io.github.mfvanek.spring.boot3.kotlin.test.support.TestBase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class DatabaseStructureStaticAnalysisTest : TestBase() {

    @Autowired
    private lateinit var checks: List<DatabaseCheckOnHost<out DbObject>>

    @Test
    @DisplayName("Always check PostgreSQL version in your tests")
    fun checkPostgresVersion() {
        val pgVersion = jdbcTemplate.queryForObject("select version();", String::class.java)
        assertThat(pgVersion)
            .startsWith("PostgreSQL 17.4")
    }

    @Test
    fun databaseStructureCheckForPublicSchema() {
        assertThat(checks)
            .hasSameSizeAs(Diagnostic.entries.toTypedArray())

        checks
            .filter { obj: DatabaseCheckOnHost<out DbObject>? -> obj!!.isStatic }
            .forEach { check: DatabaseCheckOnHost<out DbObject>? ->
                assertThat(check!!.check(PgContext.ofDefault(), SkipLiquibaseTablesPredicate.ofDefault()))
                    .`as`(check.name)
                    .isEmpty()
            }
    }
}
