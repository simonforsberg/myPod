package org.example.logging;

import jakarta.persistence.EntityManagerFactory;
import org.apache.logging.log4j.core.appender.db.jdbc.ConnectionSource;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.example.PersistenceManager;
import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.internal.SessionFactoryImpl;

import java.sql.Connection;
import java.sql.SQLException;

@Plugin(name = "JpaHibernateConnectionSource", category = "Core", printObject = true)
public class JpaHibernateConnectionSource implements ConnectionSource {

    @Override
    public Connection getConnection() throws SQLException {
        EntityManagerFactory emf = PersistenceManager.getEntityManagerFactory();
        if (emf == null) {
            throw new IllegalStateException("EntityManagerFactory not set in EMFHolder");
        }

        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        SessionFactoryImpl sfi = sessionFactory.unwrap(SessionFactoryImpl.class);
        JdbcServices jdbcServices = sfi.getServiceRegistry().getService(JdbcServices.class);
        JdbcConnectionAccess connectionAccess = jdbcServices.getBootstrapJdbcConnectionAccess();
        return connectionAccess.obtainConnection();
    }

    @PluginFactory
    public static JpaHibernateConnectionSource createConnectionSource() {
        return new JpaHibernateConnectionSource();
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
