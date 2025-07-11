package com.github.treladev.proxy;

import java.sql.*;

public class StatementProxy implements Statement {

    private final Statement originalStatement;

    public long getExecutionTime() {
        return executionTime;
    }

    private long executionTime;

    public StatementProxy(Statement originalStatement) {
        this.originalStatement = originalStatement;
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        return originalStatement.executeQuery(sql);
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return originalStatement.executeUpdate(sql);
    }

    @Override
    public void close() throws SQLException {
        originalStatement.close();
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return originalStatement.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        originalStatement.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return originalStatement.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        originalStatement.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        originalStatement.setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return originalStatement.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        originalStatement.setQueryTimeout(seconds);
    }

    @Override
    public void cancel() throws SQLException {
        originalStatement.cancel();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return originalStatement.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
        originalStatement.clearWarnings();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        originalStatement.setCursorName(name);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        long startTime = System.currentTimeMillis();
          boolean result = originalStatement.execute(sql);
        long endTime = System.currentTimeMillis();
        this.executionTime = endTime-startTime;
        return result;
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return originalStatement.getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return originalStatement.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return originalStatement.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        originalStatement.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return originalStatement.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        originalStatement.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return originalStatement.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return originalStatement.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return originalStatement.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        originalStatement.addBatch(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        originalStatement.clearBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return originalStatement.executeBatch();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return originalStatement.getConnection();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return originalStatement.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return originalStatement.getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return originalStatement.executeUpdate(sql, autoGeneratedKeys);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return originalStatement.executeUpdate(sql, columnIndexes);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return originalStatement.executeUpdate(sql, columnNames);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return originalStatement.execute(sql, autoGeneratedKeys);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return originalStatement.execute(sql, columnIndexes);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return originalStatement.execute(sql, columnNames);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return originalStatement.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return originalStatement.isClosed();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        originalStatement.setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return originalStatement.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        originalStatement.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return originalStatement.isCloseOnCompletion();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return originalStatement.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return originalStatement.isWrapperFor(iface);
    }
}