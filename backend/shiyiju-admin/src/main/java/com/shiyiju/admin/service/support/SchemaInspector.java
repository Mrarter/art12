package com.shiyiju.admin.service.support;

import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves table and column names at runtime so the admin service can work with
 * the slightly inconsistent schemas that already exist in this repo.
 */
@Component
public class SchemaInspector {

    private final DataSource dataSource;
    private final Map<String, String> tableCache = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> columnCache = new ConcurrentHashMap<>();

    public SchemaInspector(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String resolveTable(String cacheKey, String... candidates) {
        return tableCache.computeIfAbsent(cacheKey, key -> detectTable(candidates));
    }

    public boolean hasColumn(String tableName, String columnName) {
        return getColumns(tableName).contains(normalize(columnName));
    }

    public String firstExistingColumn(String tableName, String... candidates) {
        for (String candidate : candidates) {
            if (hasColumn(tableName, candidate)) {
                return candidate;
            }
        }
        return candidates.length > 0 ? candidates[0] : null;
    }

    public Set<String> getColumns(String tableName) {
        return columnCache.computeIfAbsent(tableName, this::detectColumns);
    }

    public void evictColumns(String tableName) {
        if (tableName != null) {
            columnCache.remove(tableName);
        }
    }

    private String detectTable(String... candidates) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            for (String candidate : candidates) {
                if (tableExists(metaData, catalog, candidate)) {
                    return candidate;
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to inspect database tables", ex);
        }
        return candidates.length > 0 ? candidates[0] : null;
    }

    private boolean tableExists(DatabaseMetaData metaData, String catalog, String tableName) throws SQLException {
        try (ResultSet resultSet = metaData.getTables(catalog, null, tableName, new String[] {"TABLE"})) {
            if (resultSet.next()) {
                return true;
            }
        }
        try (ResultSet resultSet = metaData.getTables(catalog, null, tableName.toUpperCase(Locale.ROOT), new String[] {"TABLE"})) {
            return resultSet.next();
        }
    }

    private Set<String> detectColumns(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String catalog = connection.getCatalog();
            Set<String> columns = new HashSet<>();
            try (ResultSet resultSet = metaData.getColumns(catalog, null, tableName, null)) {
                while (resultSet.next()) {
                    columns.add(normalize(resultSet.getString("COLUMN_NAME")));
                }
            }
            if (!columns.isEmpty()) {
                return Collections.unmodifiableSet(columns);
            }
            try (ResultSet resultSet = metaData.getColumns(catalog, null, tableName.toUpperCase(Locale.ROOT), null)) {
                while (resultSet.next()) {
                    columns.add(normalize(resultSet.getString("COLUMN_NAME")));
                }
            }
            return Collections.unmodifiableSet(columns);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to inspect columns for table: " + tableName, ex);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
