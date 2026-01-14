package org.example.logging;

import jakarta.persistence.EntityManagerFactory;

/**
 * Holds a static reference to the application's EntityManagerFactory
 * so Log4j2 can access it after initialization.
 */
public class EMFHolder {
    private static EntityManagerFactory emf;

    public static void setEntityManagerFactory(EntityManagerFactory factory) {
        emf = factory;
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        return emf;
    }
}
