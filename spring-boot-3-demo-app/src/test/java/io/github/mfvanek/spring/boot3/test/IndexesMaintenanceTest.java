package io.github.mfvanek.spring.boot3.test;

import io.github.mfvanek.pg.common.maintenance.DatabaseCheckOnHost;
import io.github.mfvanek.pg.common.maintenance.Diagnostic;
import io.github.mfvanek.pg.model.DbObject;
import io.github.mfvanek.pg.model.PgContext;
import io.github.mfvanek.pg.model.table.TableNameAware;
import io.github.mfvanek.spring.boot3.test.support.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

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
                final Predicate<DbObject> skipLiquibaseTables = dbObject -> {
                    if (dbObject instanceof TableNameAware t) {
                        return !t.getTableName().equalsIgnoreCase("databasechangelog");
                    }
                    return true;
                };
                final List<? extends DbObject> objects = check.check(PgContext.ofPublic(), skipLiquibaseTables);
                assertThat(objects)
                    .as(check.getDiagnostic().name())
                    .isEmpty();
            });
    }
}
