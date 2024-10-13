package io.github.mfvanek.spring.boot3.test;

import io.github.mfvanek.pg.common.maintenance.DatabaseCheckOnHost;
import io.github.mfvanek.pg.common.maintenance.Diagnostic;
import io.github.mfvanek.pg.model.DbObject;
import io.github.mfvanek.pg.model.column.Column;
import io.github.mfvanek.pg.model.table.Table;
import io.github.mfvanek.spring.boot3.test.support.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

class IndexesMaintenanceTest extends TestBase {

    @Autowired
    private List<DatabaseCheckOnHost<? extends DbObject>> checks;

    @Test
    @DisplayName("Always check PostgreSQL version in your tests")
    void checkPostgresVersion() {
        final String pgVersion = jdbcTemplate.queryForObject("select version();", String.class);
        assertThat(pgVersion)
            .startsWith("PostgreSQL 16.4");
    }

    @Test
    void databaseStructureCheckForPublicSchema() {
        assertThat(checks)
            .hasSameSizeAs(Diagnostic.values());

        checks.forEach(check -> {
            switch (check.getDiagnostic()) {
                case TABLES_WITHOUT_PRIMARY_KEY, TABLES_WITHOUT_DESCRIPTION -> assertThat(check.check())
                    .asInstanceOf(list(Table.class))
                    .hasSize(1)
                    .containsExactly(Table.of("databasechangelog", 0L));

                case COLUMNS_WITHOUT_DESCRIPTION -> assertThat(check.check())
                    .asInstanceOf(list(Column.class))
                    .hasSize(14)
                    .allSatisfy(column -> assertThat(column.getTableName()).isEqualTo("databasechangelog"));

                case TABLES_WITH_MISSING_INDEXES -> assertThat(check.check())
                    .hasSizeLessThanOrEqualTo(1); // TODO skip runtime checks after https://github.com/mfvanek/pg-index-health/issues/456

                default -> assertThat(check.check())
                    .as(check.getDiagnostic().name())
                    .isEmpty();
            }
        });
    }
}
