// package com.pfh.user.functionalitu.test;

// import javax.sql.DataSource;

// import org.springframework.boot.jdbc.EmbeddedDatabaseBuilder;
// import org.springframework.boot.jdbc.EmbeddedDatabaseType;
// import org.springframework.boot.test.context.TestConfiguration;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Primary;

// @TestConfiguration
// public class TestConfig {

//     /**
//      * Override the default DataSource bean with an embedded H2 database
//      * so that integration tests run against an in-memory DB.
//      */
//     @Bean
//     @Primary
//     public DataSource dataSource() {
//         return new EmbeddedDatabaseBuilder()
//                 .setType(EmbeddedDatabaseType.H2)
//                 .build();
//     }
// }