package com.enterprise.report.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum DataSourceType {
    MYSQL,
    POSTGRESQL,
    CLICKHOUSE,
    ORACLE,
    SQLSERVER,
    API,
    EXCEL,
    CSV,
    JSON_FILE,
    MINIO,
    FILE;

    @JsonCreator
    public static DataSourceType fromString(String value) {
        if (value == null) return null;
        try {
            return valueOf(value.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            // Handle common aliases
            return switch (value.toLowerCase()) {
                case "mysql" -> MYSQL;
                case "postgresql", "postgres", "pg" -> POSTGRESQL;
                case "clickhouse", "ch" -> CLICKHOUSE;
                case "oracle", "ora" -> ORACLE;
                case "sqlserver", "mssql", "sql_server" -> SQLSERVER;
                case "excel", "xlsx", "xls" -> EXCEL;
                case "csv" -> CSV;
                case "json" -> JSON_FILE;
                case "file", "upload" -> FILE;
                case "api" -> API;
                case "minio" -> MINIO;
                default -> FILE;
            };
        }
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}
