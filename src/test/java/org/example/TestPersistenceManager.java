package org.example;

import jakarta.persistence.EntityManagerFactory;

import java.util.Map;

public final class TestPersistenceManager {

    private static EntityManagerFactory emf;

    private TestPersistenceManager() {
    }

    public static EntityManagerFactory get() {
        if (emf == null) {
            emf = EntityManagerFactoryProvider.create(
                "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                "sa",
                "",
                Map.of(
                    "hibernate.hbm2ddl.auto", "create-drop",
                    "hibernate.dialect", "org.hibernate.dialect.H2Dialect",
                    "hibernate.show_sql", "false"
                )
            );
        }
        return emf;
    }

    public static void close() {
        if (emf != null) {
            emf.close();
            emf = null;
        }
    }
}

