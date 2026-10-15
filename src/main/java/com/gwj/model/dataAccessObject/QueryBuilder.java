package com.gwj.model.dataAccessObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueryBuilder {
    private String table;
    private String type;
    private List<String> columns = new ArrayList<>();
    private List<String> setClauses = new ArrayList<>();
    private String whereClause;
    private StringBuilder joins = new StringBuilder();
    private String selectColumns = "*";

    public QueryBuilder insertInto(String table) {
        this.type = "INSERT";
        this.table = table;
        return this;
    }

    public QueryBuilder update(String table) {
        this.type = "UPDATE";
        this.table = table;
        return this;
    }

    public QueryBuilder deleteFrom(String table) {
        this.type = "DELETE";
        this.table = table;
        return this;
    }

    public QueryBuilder select(String columns) {
        this.type = "SELECT";
        this.selectColumns = columns;
        return this;
    }

    public QueryBuilder from(String table) {
        this.table = table;
        return this;
    }

    public QueryBuilder columns(List<String> columns) {
        this.columns = columns;
        return this;
    }

    public QueryBuilder set(List<String> setClauses) {
        this.setClauses = setClauses;
        return this;
    }

    public QueryBuilder where(String whereClause) {
        this.whereClause = whereClause;
        return this;
    }

    public QueryBuilder innerJoin(String table, String onCondition) {
        this.joins.append(" INNER JOIN ").append(table).append(" ON ").append(onCondition);
        return this;
    }

    private String sanitize(String identifier) {
        if (identifier == null) return "";
        return identifier.replace("`", "");
    }

    public String build() {
        StringBuilder sql = new StringBuilder();
        String cleanTable = sanitize(table);

        switch (type) {
            case "INSERT":
                sql.append("INSERT INTO ").append(cleanTable).append(" (");
                if (columns != null && !columns.isEmpty()) {
                    List<String> cleanCols = columns.stream().map(this::sanitize).toList();
                    sql.append(String.join(", ", cleanCols));
                }
                sql.append(") VALUES (");
                if (columns != null && !columns.isEmpty()) {
                    sql.append(String.join(", ", Collections.nCopies(columns.size(), "?")));
                }
                sql.append(")");
                break;

            case "UPDATE":
                sql.append("UPDATE ").append(cleanTable).append(" SET ");
                if (setClauses != null && !setClauses.isEmpty()) {
                    List<String> cleanSet = setClauses.stream().map(this::sanitize).toList();
                    sql.append(String.join(", ", cleanSet));
                }
                if (whereClause != null && !whereClause.isEmpty()) {
                    sql.append(" WHERE ").append(sanitize(whereClause));
                }
                break;

            case "DELETE":
                sql.append("DELETE FROM ").append(cleanTable);
                if (whereClause != null && !whereClause.isEmpty()) {
                    sql.append(" WHERE ").append(sanitize(whereClause));
                }
                break;

            case "SELECT":
                sql.append("SELECT ").append(selectColumns).append(" FROM ").append(cleanTable);
                if (joins.length() > 0) {
                    sql.append(sanitize(joins.toString()));
                }
                if (whereClause != null && !whereClause.isEmpty()) {
                    String wc = sanitize(whereClause).trim();
                    if (!wc.toUpperCase().startsWith("WHERE")) {
                        sql.append(" WHERE ");
                    } else {
                        sql.append(" ");
                    }
                    sql.append(wc);
                }
                break;
        }
        return sql.toString();
    }
}
