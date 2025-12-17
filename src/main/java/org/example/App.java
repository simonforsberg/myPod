package org.example;

import jakarta.persistence.EntityManagerFactory;

public class App {
    public static void main(String[] args) {
        ItunesApiClient apiClient = new ItunesApiClient();
        SongRepository songRepo = new SongRepositoryImpl();

        try {
            EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();
            if (!emf.isOpen()) {
                throw new IllegalStateException("EntityManagerFactory is not open");
            }
            DatabaseInitializer initializer = new DatabaseInitializer(apiClient, songRepo);
            initializer.init();
            System.out.println("Database initialization completed successfully");
        } catch (Exception e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            throw new RuntimeException("Failed to initialize database", e);
        }
    }
}
