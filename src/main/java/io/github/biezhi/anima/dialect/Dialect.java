package io.github.biezhi.anima.dialect;

import io.github.biezhi.anima.core.SQLParams;
import io.github.biezhi.anima.exception.AnimaException;
import io.github.biezhi.anima.utils.AnimaUtils;

import java.lang.reflect.Field;

import static io.github.biezhi.anima.utils.AnimaUtils.isIgnore;

/**
 * Database Dialect
 *
 * @author biezhi
 * @date 2018/3/17
 */
public interface Dialect {

    default String select(SQLParams sqlParams) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT");
        if (AnimaUtils.isNotEmpty(sqlParams.getSelectColumns())) {
            sql.append(' ').append(sqlParams.getSelectColumns()).append(' ');
        } else if (AnimaUtils.isNotEmpty(sqlParams.getExcludedColumns())) {
            sql.append(' ').append(AnimaUtils.buildColumns(sqlParams.getExcludedColumns(), sqlParams.getModelClass())).append(' ');
        } else {
            sql.append(" * ");
        }
        sql.append("FROM ").append(sqlParams.getTableName());
        if (sqlParams.getConditionSQL().length() > 0) {
            sql.append(" WHERE ").append(sqlParams.getConditionSQL().substring(5));
        }
        if (AnimaUtils.isNotEmpty(sqlParams.getOrderBy())) {
            sql.append(" ORDER BY ").append(sqlParams.getOrderBy());
        }
        return sql.toString();
    }

    default String count(SQLParams sqlParams) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) FROM ").append(sqlParams.getTableName());
        if (sqlParams.getConditionSQL().length() > 0) {
            sql.append(" WHERE ").append(sqlParams.getConditionSQL().substring(5));
        }
        return sql.toString();
    }

    default String insert(SQLParams sqlParams) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(sqlParams.getTableName());

        StringBuilder columnNames = new StringBuilder();
        StringBuilder placeholder = new StringBuilder();

        for (Field field : sqlParams.getModelClass().getDeclaredFields()) {
            if (isIgnore(field)) {
                continue;
            }
            columnNames.append(",").append(AnimaUtils.toColumnName(field));
            placeholder.append(",?");
        }
        sql.append("(").append(columnNames.substring(1)).append(")").append(" VALUES (")
                .append(placeholder.substring(1)).append(")");
        return sql.toString();
    }

    default String update(SQLParams sqlParams) {
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(sqlParams.getTableName()).append(" SET ");

        StringBuilder setSQL = new StringBuilder();

        if (null != sqlParams.getUpdateColumns() && !sqlParams.getUpdateColumns().isEmpty()) {
            sqlParams.getUpdateColumns().forEach((key, value) -> setSQL.append(key).append(" = ?, "));
        } else {
            for (Field field : sqlParams.getModelClass().getDeclaredFields()) {
                try {
                    if (isIgnore(field)) {
                        continue;
                    }
                    field.setAccessible(true);
                    Object value = field.get(sqlParams.getModel());
                    if (null == value) {
                        continue;
                    }
                    setSQL.append(AnimaUtils.toColumnName(field.getName())).append(" = ?, ");
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new AnimaException("illegal argument or Access:", e);
                }
            }
        }
        sql.append(setSQL.substring(0, setSQL.length() - 2));
        if (sqlParams.getConditionSQL().length() > 0) {
            sql.append(" WHERE ").append(sqlParams.getConditionSQL().substring(5));
        }
        return sql.toString();
    }

    default String delete(SQLParams sqlParams) {
        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(sqlParams.getTableName());

        if (sqlParams.getConditionSQL().length() > 0) {
            sql.append(" WHERE ").append(sqlParams.getConditionSQL().substring(5));
        } else {
            StringBuilder columnNames = new StringBuilder();
            for (Field field : sqlParams.getModelClass().getDeclaredFields()) {
                if (isIgnore(field)) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(sqlParams.getModel());
                    if (null == value) {
                        continue;
                    }
                    columnNames.append(AnimaUtils.toColumnName(field.getName())).append(" = ? and ");
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new AnimaException("illegal argument or Access:", e);
                }
            }
            if (columnNames.length() > 0) {
                sql.append(" WHERE ").append(columnNames.substring(0, columnNames.length() - 5));
            }
        }
        return sql.toString();
    }

    String paginate(SQLParams sqlParams);

}
