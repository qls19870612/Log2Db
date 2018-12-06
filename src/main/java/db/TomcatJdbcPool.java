package db;


import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

import game.service.db.DBPool;

public class TomcatJdbcPool implements DBPool {

    private static final Logger logger = LoggerFactory.getLogger(TomcatJdbcPool.class);

    private final DataSource datasource;

    final String url;
    final String username;
    final String password;

    public TomcatJdbcPool(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;

        PoolProperties p = new PoolProperties();
        url = "jdbc:mysql://" + url +
                "?rewriteBatchedStatements=true&cachePrepStmts=true&prepStmtCacheSize=100&autoReconnect=true&initialTimeout=1&failOverReadOnly=false&autoReconnectForPools=true&noAccessToProcedureBodies=true&useUnicode=true&characterEncoding=UTF-8";

        p.setUrl(url);
        p.setDriverClassName("com.mysql.jdbc.Driver");
        p.setUsername(username);
        p.setPassword(password);
        p.setJmxEnabled(false);
        p.setTestWhileIdle(true);
        p.setTestOnBorrow(true);
        p.setValidationQuery("select 1");
        p.setTestOnReturn(false);
        p.setValidationInterval(30000);
        p.setTimeBetweenEvictionRunsMillis(30000);
        p.setMaxActive(500);
        p.setInitialSize(10);
        p.setMaxWait(30000);
        p.setRemoveAbandonedTimeout(60);

        p.setMinEvictableIdleTimeMillis(30000);
        p.setMinIdle(10);
        p.setLogAbandoned(false);
        p.setRemoveAbandoned(true);
        p.setDefaultAutoCommit(true);
        p.setFairQueue(false);

        datasource = new DataSource(p);
    }

    @Override
    public Connection getConnection() {

        try {
            Connection conn = datasource.getConnection();
            if (conn == null) {
                logger.error("TomcatJdbcPool.getConnection conn == null");
                conn = datasource.getConnection();
                if (conn == null) {
                    logger.error("TomcatJdbcPool.getConnection retry conn == null");
                }
            }

            return conn;
        } catch (SQLException e) {
            logger.error("TomcatJdbcPool.getConnection", e);
        }

        try {
            Connection conn = datasource.getConnection();
            if (conn == null) {
                logger.error("TomcatJdbcPool.getConnection conn == null retry");
                conn = datasource.getConnection();
                if (conn == null) {
                    logger.error("TomcatJdbcPool.getConnection retry conn == null retry");
                }
            }

            return conn;
        } catch (SQLException e) {
            logger.error("TomcatJdbcPool.getConnection retry", e);
        }

        return null;
    }

    @Override
    public void close() {
        datasource.close(true);
    }
}
