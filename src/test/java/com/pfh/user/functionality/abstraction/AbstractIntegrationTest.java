package com.pfh.user.functionality.abstraction;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
public abstract class AbstractIntegrationTest {

    // Singleton PostgreSQL container
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    // Singleton Redis container
    private static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("redis:7"))
                    .withExposedPorts(6379);

    static {
        POSTGRES_CONTAINER.start();

        // Map Redis port to a fixed port on the host for easier access
        REDIS_CONTAINER.setPortBindings(java.util.List.of("6379:6379"));
        REDIS_CONTAINER.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES_CONTAINER::getDriverClassName);

        registry.add("spring.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
    }

    protected static void pauseContainer(){
        POSTGRES_CONTAINER.getDockerClient().pauseContainerCmd(POSTGRES_CONTAINER.getContainerId()).exec();
    } 

    protected static void unpauseContainer(){
        POSTGRES_CONTAINER.getDockerClient().unpauseContainerCmd(POSTGRES_CONTAINER.getContainerId()).exec();
    } 

}
