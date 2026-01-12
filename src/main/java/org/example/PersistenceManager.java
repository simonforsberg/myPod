package org.example;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceConfiguration;

import java.util.List;
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
