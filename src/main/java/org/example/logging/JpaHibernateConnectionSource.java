package org.example.logging;

import jakarta.persistence.EntityManagerFactory;
import org.apache.logging.log4j.core.appender.db.jdbc.ConnectionSource;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.SessionFactoryImpl;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Log4j2 ConnectionSource that uses a JPA EntityManagerFactory's Hibernate connection pool.
 * Works with Hibernate 6.x.
 */
@Plugin(name = "JpaHibernateConnectionSource", category = "Core", printObject = true)
public class JpaHibernateConnectionSource implements ConnectionSource {

    private final EntityManagerFactory emf;

    public JpaHibernateConnectionSource(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (emf == null) {
            throw new IllegalStateException("EntityManagerFactory is not initialized yet.");
        }

        // Unwrap JPA EntityManagerFactory to Hibernate SessionFactory
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);

        // Get Hibernate's internal SessionFactoryImpl
        SessionFactoryImpl sfi = sessionFactory.unwrap(SessionFactoryImpl.class);

        // Get JDBC services
        JdbcServices jdbcServices = sfi.getServiceRegistry().getService(JdbcServices.class);

        // Get connection access object
        JdbcConnectionAccess connectionAccess = jdbcServices.getBootstrapJdbcConnectionAccess();

        // Return a pooled connection
        return connectionAccess.obtainConnection();
    }

    @Override
    public String toString() {
        return "JpaHibernateConnectionSource using Hibernate's pool";
    }

    /**
     * Factory method for Log4j2 to create the plugin instance.
     * This uses a static holder to retrieve the EMF after application startup.
     */
    @PluginFactory
    public static JpaHibernateConnectionSource createConnectionSource() {
        return new JpaHibernateConnectionSource(EMFHolder.getEntityManagerFactory());
    }

    @Override
    public State getState() {
        return null;
    }

    @Override
    public void initialize() {

    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public boolean isStarted() {
        return false;
    }

    @Override
    public boolean isStopped() {
        return false;
    }
}
