package parkinglot.mcp;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class DatasourceProfileTest {

    @ParameterizedTest
    @CsvSource({
            "prod, PROD_DATASOURCE",
            "staging, STAGING_DATASOURCE"
    })
    void usesApiServiceDatasourceVariablesAndPreservesReadOnlySettings(String profile, String prefix) {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues(
                        "spring.profiles.active=" + profile,
                        prefix + "_URL=jdbc:postgresql://database:5432/analytics?sslmode=require",
                        prefix + "_USERNAME=analytics-reader",
                        prefix + "_PASSWORD=test-password")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var environment = context.getEnvironment();
                    assertThat(environment.getProperty("spring.datasource.url"))
                            .isEqualTo("jdbc:postgresql://database:5432/analytics?sslmode=require");
                    assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("analytics-reader");
                    assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("test-password");
                    assertThat(environment.getProperty("spring.datasource.hikari.read-only")).isEqualTo("true");
                    assertThat(environment.getProperty("spring.datasource.hikari.data-source-properties.readOnlyMode"))
                            .isEqualTo("always");
                    assertThat(environment.getProperty("spring.sql.init.mode")).isEqualTo("never");
                });
    }
}
