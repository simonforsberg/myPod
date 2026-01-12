package org.example;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceConfiguration;

import java.util.List;
import java.util.Map;

public class EntityManagerFactoryProvider {

    public static EntityManagerFactory create(
        String jdbcUrl,
        String username,
        String password,
        Map<String, String> extraProps
    ) {
        List<Class<?>> entities = scanEntities("org.example.entity");

        PersistenceConfiguration cfg =
            new HibernatePersistenceConfiguration("emf")
                .jdbcUrl(jdbcUrl)
                .jdbcUsername(username)
                .jdbcPassword(password)
                .managedClasses(entities);

        extraProps.forEach(cfg::property);

        return cfg.createEntityManagerFactory();
    }

    private static List<Class<?>> scanEntities(String pkg) {
        try (ScanResult scanResult =
                 new ClassGraph()
                     .enableAnnotationInfo()
                     .acceptPackages(pkg)
                     .scan()) {

            return scanResult.getClassesWithAnnotation(Entity.class).loadClasses();
        }
    }
}
