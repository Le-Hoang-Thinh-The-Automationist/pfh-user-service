package com.pfh.user.functionality.abstraction;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
public abstract class AbstractIntegrationTest {

    // Singleton container, runs for the lifetime of the JVM
    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:15"))
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    static {
        // Start container only once
        POSTGRES_CONTAINER.start();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES_CONTAINER::getDriverClassName);
    }

    protected static void pauseContainer(){
        POSTGRES_CONTAINER.getDockerClient().pauseContainerCmd(POSTGRES_CONTAINER.getContainerId()).exec();
    } 

    protected static void unpauseContainer(){
        POSTGRES_CONTAINER.getDockerClient().unpauseContainerCmd(POSTGRES_CONTAINER.getContainerId()).exec();
    } 

}
