package com.example.logging;

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
 * Log4j2 ConnectionSource that uses Hibernate 6's connection acquisition API.
 */
@Plugin(name = "HibernateConnectionSource", category = "Core", printObject = true)
public class HibernateConnectionSource implements ConnectionSource {

    private final SessionFactory sessionFactory;

    public HibernateConnectionSource(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Connection getConnection() throws SQLException {
        // Unwrap to Hibernate's internal SessionFactoryImpl
        SessionFactoryImpl sfi = sessionFactory.unwrap(SessionFactoryImpl.class);

        // Get JDBC services
        JdbcServices jdbcServices = sfi.getServiceRegistry().getService(JdbcServices.class);

        // Get connection access object
        assert jdbcServices != null;
        JdbcConnectionAccess connectionAccess = jdbcServices.getBootstrapJdbcConnectionAccess();

        // Get a connection from Hibernate's pool
        return connectionAccess.obtainConnection();
    }

    @Override
    public String toString() {
        return "HibernateConnectionSource using Hibernate 6.x pool";
    }

    @PluginFactory
    public static HibernateConnectionSource createConnectionSource() {
        return new HibernateConnectionSource(com.example.logging.HibernateUtil.getSessionFactory());
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
