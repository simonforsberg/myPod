package org.example;

import jakarta.persistence.EntityManagerFactory;

import java.util.Map;

public class PersistenceManager {
    private static final EntityManagerFactory emf =
        EntityManagerFactoryProvider.create(
            "jdbc:mysql://localhost:3306/myPodDB",
            "user",
            "pass",
            Map.of(
                "hibernate.hbm2ddl.auto", "update",
                "hibernate.show_sql", "true",
                "hibernate.format_sql", "true"
            )
        );

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(emf::close));
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }
}
