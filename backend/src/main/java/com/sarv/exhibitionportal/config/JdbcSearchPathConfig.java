package com.sarv.exhibitionportal.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Flyway and POC tables live in {@code exhibition_portal}. Hikari sets search_path in
 * production; embedded-Postgres tests replace that DataSource, so wrap every
 * {@code dataSource} bean and set it on checkout.
 */
@Configuration
public class JdbcSearchPathConfig {

    static final String SEARCH_PATH_SQL = "SET search_path TO exhibition_portal, public";

    @Bean
    static BeanPostProcessor exhibitionPortalSearchPath() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource
                        && "dataSource".equals(beanName)
                        && !(bean instanceof SearchPathDataSource)) {
                    return new SearchPathDataSource(dataSource);
                }
                return bean;
            }
        };
    }

    static final class SearchPathDataSource extends DelegatingDataSource {
        SearchPathDataSource(DataSource target) {
            super(target);
        }

        @Override
        public Connection getConnection() throws SQLException {
            return withSearchPath(super.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return withSearchPath(super.getConnection(username, password));
        }

        private static Connection withSearchPath(Connection connection) throws SQLException {
            try (Statement statement = connection.createStatement()) {
                statement.execute(SEARCH_PATH_SQL);
            }
            return connection;
        }
    }
}
