package io.github.mfvanek.spring.boot3.test;

import io.github.mfvanek.pg.common.maintenance.DatabaseCheckOnHost;
import io.github.mfvanek.pg.common.maintenance.Diagnostic;
import io.github.mfvanek.pg.model.DbObject;
import io.github.mfvanek.pg.model.PgContext;
import io.github.mfvanek.pg.model.column.Column;
import io.github.mfvanek.spring.boot3.test.support.TestBase;
import org.assertj.core.api.ListAssert;
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
            .startsWith("PostgreSQL 17.0");
    }

    @Test
    void databaseStructureCheckForPublicSchema() {
        assertThat(checks)
            .hasSameSizeAs(Diagnostic.values());

        checks.stream()
            .filter(DatabaseCheckOnHost::isStatic)
            .forEach(check -> {
                final List<? extends DbObject> objects = check.check(PgContext.ofPublic(), o -> !o.getName().equalsIgnoreCase("databasechangelog"));
                final ListAssert<? extends DbObject> checkAssert = assertThat(objects)
                    .as(check.getDiagnostic().name());

                if (check.getDiagnostic() == Diagnostic.COLUMNS_WITHOUT_DESCRIPTION) {
                    assertThat(objects)
                        .asInstanceOf(list(Column.class))
                        .hasSize(14)
                        .allSatisfy(column -> assertThat(column.getTableName()).isEqualTo("databasechangelog"));
                } else {
                    checkAssert.isEmpty();
                }
            });
    }
}
