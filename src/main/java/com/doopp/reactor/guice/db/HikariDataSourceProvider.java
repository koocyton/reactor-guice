package com.doopp.reactor.guice.db;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class HikariDataSourceProvider implements Provider<DataSource> {

    private HikariDataSource dataSource = new HikariDataSource();

    @Inject
    public void setDriverClassName(@Named("JDBC.driverClassName") final String driverClassName) {
        dataSource.setDriverClassName(driverClassName);
    }

    @Inject
    public void setUrl(@Named("JDBC.url") final String url) {
        dataSource.setJdbcUrl(url);
    }

    @Inject
    public void setUsername(@Named("JDBC.username") final String username) {
        dataSource.setUsername(username);
    }

    @Inject
    public void setPassword(@Named("JDBC.password") final String password) {
        dataSource.setPassword(password);
    }

    @Inject
    public void setMinimumIdle(@Named("JDBC.minimumIdle") final int minimumIdle) {
        dataSource.setMinimumIdle(minimumIdle);
    }

    @Inject
    public void setMaximumPoolSize(@Named("JDBC.maximumPoolSize") final int maximumPoolSize) {
        dataSource.setMaximumPoolSize(maximumPoolSize);
    }

    @Inject
    public void setConnectionTestQuery(@Named("JDBC.connectionTestQuery") final String connectionTestQuery) {
        dataSource.setConnectionTestQuery(connectionTestQuery);
    }

    @Inject
    public void setDataSourceCachePrepStmts(@Named("JDBC.dataSource.cachePrepStmts") final boolean cachePrepStmts) {
        dataSource.addDataSourceProperty("cachePrepStmts", cachePrepStmts);
    }

    @Inject
    public void setDataSourcePrepStmtCacheSize(@Named("JDBC.dataSource.prepStmtCacheSize") final int prepStmtCacheSize) {
        dataSource.addDataSourceProperty("prepStmtCacheSize", prepStmtCacheSize);
    }

    @Inject
    public void setDataPrepStmtCacheSqlLimit(@Named("JDBC.dataSource.prepStmtCacheSqlLimit") final int prepStmtCacheSqlLimit) {
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", prepStmtCacheSqlLimit);
    }

    @Inject
    public void setDataUseServerPrepStmts(@Named("JDBC.dataSource.useServerPrepStmts") final boolean useServerPrepStmts) {
        dataSource.addDataSourceProperty("useServerPrepStmts", useServerPrepStmts);
    }

    @Override
    public DataSource get() {
        return dataSource;
    }
}
