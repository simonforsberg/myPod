package org.example;

import jakarta.persistence.EntityManagerFactory;

import java.util.Map;

/**
 * Provides a lazily initialized {@link EntityManagerFactory} for test execution.
 *
 * <p>This class is intended exclusively for use in automated tests. It creates
 * an in-memory H2 database with a schema lifecycle tailored for testing
 * (create-drop), ensuring full isolation between test runs.</p>
 *
 * <p>The {@link EntityManagerFactory} is initialized on first access and
 * reused for the duration of a test suite. It should be explicitly closed
 * after each test (or test class) to guarantee a clean persistence state.</p>
 */
public final class TestPersistenceManager {

    /** Singleton {@link EntityManagerFactory} instance for tests. */
    private static EntityManagerFactory emf;

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This class is a static utility and should never be instantiated.</p>
     */
    private TestPersistenceManager() {
    }

    /**
     * Returns the test {@link EntityManagerFactory}, creating it if necessary.
     *
     * <p>The factory is configured to use an in-memory H2 database with
     * automatic schema creation and teardown. The database remains alive
     * for the duration of the JVM to support multiple transactions per test.</p>
     *
     * @return a configured {@link EntityManagerFactory} for testing purposes
     */
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

    /**
     * Closes the test {@link EntityManagerFactory} and releases all resources.
     *
     * <p>This method should be called after each test or test suite to ensure
     * a clean shutdown of the persistence context and prevent state leakage
     * between tests.</p>
     */
    public static void close() {
        if (emf != null) {
            emf.close();
            emf = null;
        }
    }
}

