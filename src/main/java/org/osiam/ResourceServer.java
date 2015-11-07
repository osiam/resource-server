package org.osiam;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.google.common.collect.ImmutableMap;
import com.ryantenney.metrics.spring.config.annotation.EnableMetrics;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurerAdapter;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.osiam.security.authorization.AccessTokenValidationService;
import org.osiam.security.authorization.OsiamMethodSecurityExpressionHandler;
import org.osiam.security.helper.SSLRequestLoggingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
import org.springframework.security.oauth2.provider.error.OAuth2AuthenticationEntryPoint;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.Filter;
import javax.sql.DataSource;
import java.util.Map;

@SpringBootApplication
@EnableWebMvc
@EnableWebSecurity
@EnableTransactionManagement
@EnableMetrics
@PropertySource("classpath:/resource-server.properties")
public class ResourceServer extends SpringBootServletInitializer {

    private static final Map<String, Object> NAMING_STRATEGY = ImmutableMap.<String, Object> of(
            "spring.jpa.hibernate.naming_strategy", "org.hibernate.cfg.ImprovedNamingStrategy");

    @Value("${org.osiam.resource-server.db.driver}")
    private String driverClassName;

    @Value("${org.osiam.resource-server.db.url}")
    private String databaseUrl;

    @Value("${org.osiam.resource-server.db.username}")
    private String databaseUserName;

    @Value("${org.osiam.resource-server.db.password}")
    private String databasePassword;

    @Value("${org.osiam.resource-server.db.vendor}")
    private String databaseVendor;

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ResourceServer.class);
        application.setDefaultProperties(NAMING_STRATEGY);
        application.run(args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        application.application().setDefaultProperties(NAMING_STRATEGY);
        return application.sources(ResourceServer.class);
    }

    @Bean
    public Filter characterEncodingFilter() {
        CharacterEncodingFilter characterEncodingFilter = new CharacterEncodingFilter();
        characterEncodingFilter.setEncoding("UTF-8");
        characterEncodingFilter.setForceEncoding(true);
        return characterEncodingFilter;
    }

    @Bean
    public Filter sslRequestLoggingFilter() {
        return new SSLRequestLoggingFilter();
    }

    @Primary
    @Bean
    public DataSource dataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("osiam-resource-server-cp");
        hikariConfig.setDriverClassName(driverClassName);
        hikariConfig.setJdbcUrl(databaseUrl);
        hikariConfig.setUsername(databaseUserName);
        hikariConfig.setPassword(databasePassword);
        return new HikariDataSource(hikariConfig);
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway() {
        Flyway flyway = new Flyway();
        flyway.setBaselineOnMigrate(true);
        flyway.setDataSource(dataSource());
        flyway.setLocations("db/migration/" + databaseVendor);
        flyway.setTable("resource_server_schema_version");
        MigrationVersion version = MigrationVersion.fromVersion("0");
        flyway.setBaselineVersion(version);
        return flyway;
    }

    @Bean
    public ShaPasswordEncoder passwordEncoder() {
        ShaPasswordEncoder passwordEncoder = new ShaPasswordEncoder(512);
        passwordEncoder.setIterations(1000);
        return passwordEncoder;
    }

    @Configuration
    @EnableResourceServer
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        @Autowired
        private AccessTokenValidationService accessTokenValidationService;

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) {
            resources.resourceId("oauth2res")
                    .tokenServices(accessTokenValidationService)
                    .expressionHandler(new OsiamMethodSecurityExpressionHandler());
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.exceptionHandling()
                    .accessDeniedHandler(oauthAccessDeniedHandler())
                    .and()
                    .authorizeRequests()
                    .antMatchers("/ServiceProviderConfigs")
                    .permitAll()
                    .antMatchers("/me/**")
                    .access("#osiam.hasScopeForHttpMethod() or #oauth2.hasScope('ADMIN') or #oauth2.hasScope('ME')")
                    .antMatchers(HttpMethod.POST, "/Users/**")
                    .access("#osiam.hasScopeForHttpMethod() or #oauth2.hasScope('ADMIN')")
                    .regexMatchers(HttpMethod.GET, "/Users/?")
                    .access("#osiam.hasScopeForHttpMethod() or #oauth2.hasScope('ADMIN')")
                    .antMatchers("/Users/**")
                    .access("#osiam.hasScopeForHttpMethod() or #oauth2.hasScope('ADMIN') or " +
                            "#oauth2.hasScope('ME') and #osiam.isOwnerOfResource()")
                    .anyRequest()
                    .access("#osiam.hasScopeForHttpMethod() or #oauth2.hasScope('ADMIN')");
        }

        @Bean
        public OAuth2AuthenticationEntryPoint entryPoint() {
            OAuth2AuthenticationEntryPoint entryPoint = new OAuth2AuthenticationEntryPoint();
            entryPoint.setRealmName("oauth2-resource-server");
            return entryPoint;
        }

        @Bean
        public OAuth2AccessDeniedHandler oauthAccessDeniedHandler() {
            return new OAuth2AccessDeniedHandler();
        }
    }

    @Configuration
    @EnableMetrics
    protected static class MetricsConfiguration extends MetricsConfigurerAdapter {

        @Override
        public void configureReporters(MetricRegistry metricRegistry) {
            metricRegistry.register("jvm.memory", new MemoryUsageGaugeSet());
        }
    }
}
