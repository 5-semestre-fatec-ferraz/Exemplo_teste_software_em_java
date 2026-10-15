package com.gwj.model.repository;

import com.gwj.model.domain.IEntity;
import com.gwj.model.dataAccessObject.DataMapper;
import com.gwj.model.dataAccessObject.QueryBuilder;
import com.gwj.service.transaction.UnitOfWork;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinTable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class GenericRepository<T extends IEntity> implements IRepository<T> {
    private final Class<T> entityClass;

    public GenericRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public T save(T entity) {
        try {
            Connection conn = UnitOfWork.getConnection();
            List<Class<?>> hierarchy = DataMapper.getEntityHierarchy(entity.getClass());
            Long lastId = null;
            for (Class<?> clazz : hierarchy) {
                lastId = insertForClass(conn, entity, clazz, lastId);
            }
            entity.setId(lastId);
            saveAssociations(conn, entity);
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao inserir registro via Repositório: " + e.getMessage(), e);
        }
    }

    private Long insertForClass(Connection conn, IEntity entity, Class<?> clazz, Long parentId) throws Exception {
        ArrayList<String> columns = new ArrayList<>();
        ArrayList<Object> values = new ArrayList<>();
        ArrayList<String> placeholders = new ArrayList<>();

        if (parentId != null) {
            columns.add("id");
            placeholders.add("?");
            values.add(parentId);
        }

        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            if (DataMapper.isPersistentGetter(clazz, method)) {
                Object value = method.invoke(entity);
                if (value == null) continue;
                boolean isEntity = value instanceof IEntity;
                String colName = DataMapper.getColumnName(clazz, method, isEntity);
                if (isEntity) {
                    value = ((IEntity) value).getId();
                }
                columns.add(colName);
                placeholders.add("?");
                values.add(value);
            }
        }

        String tableName = DataMapper.getTableName(clazz);
        String sql = new QueryBuilder().insertInto(tableName).columns(columns).build();

        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < values.size(); i++) {
                pstmt.setObject(i + 1, values.get(i));
            }
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return parentId;
    }

    @Override
    public List<T> query(T entity) {
        List<T> list = new ArrayList<>();
        try {
            Connection conn = UnitOfWork.getConnection();
            String where = buildWhereClause(entity);
            String tableName = DataMapper.getTableName(entity.getClass());
            String sql = new QueryBuilder().select("*").from(tableName).where(where).build();

            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    @SuppressWarnings("unchecked")
                    T instance = (T) entity.getClass().getDeclaredConstructor().newInstance();
                    fillEntityRecursively(instance, entity.getClass(), rs, conn);
                    processAssociations(instance, conn);
                    list.add(instance);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Erro ao consultar registros via Repositório: " + e.getMessage(), e);
        }
        return list;
    }

    private void fillEntityRecursively(IEntity instance, Class<?> currentClazz, ResultSet rs, Connection conn) throws Exception {
        Method[] methods = currentClazz.getDeclaredMethods();
        for (Method method : methods) {
            if (DataMapper.isSetter(method)) {
                Class<?> paramType = method.getParameterTypes()[0];
                if (Collection.class.isAssignableFrom(paramType)) continue;

                boolean isEntity = IEntity.class.isAssignableFrom(paramType);
                String columnName = DataMapper.getColumnName(currentClazz, method, isEntity);
                
                String propName = method.getName().startsWith("is") ? method.getName().substring(2) : method.getName().substring(3);
                String fieldName = propName.substring(0, 1).toLowerCase() + propName.substring(1);
                Field targetField = DataMapper.getFieldInHierarchy(currentClazz, fieldName);
                
                try {
                    Object value = rs.getObject(columnName);
                    if (value != null) {
                        Object convertedValue;
                        if (IEntity.class.isAssignableFrom(paramType)) {
                            IEntity childInstance = (IEntity) paramType.getDeclaredConstructor().newInstance();
                            long childId = ((Number) value).longValue();
                            Method setId = DataMapper.getMethodInHierarchy(paramType, "setId", Long.class);
                            if (setId == null) setId = DataMapper.getMethodInHierarchy(paramType, "setId", long.class);
                            if (setId != null) setId.invoke(childInstance, childId);
                            readEntityComplete(childInstance, conn);
                            convertedValue = childInstance;
                        } else {
                            convertedValue = DataMapper.convertToTargetType(value, paramType, targetField);
                        }
                        method.invoke(instance, convertedValue);
                    }
                } catch (SQLException e) {
                    // Ignora se a coluna não constar neste ResultSet
                }
            }
        }

        Class<?> superClass = currentClazz.getSuperclass();
        if (superClass != null && IEntity.class.isAssignableFrom(superClass) && superClass != Object.class) {
            Long idObj = instance.getId();
            long currentId = (idObj != null) ? idObj : 0L;
            if (currentId <= 0) {
                try {
                    currentId = rs.getLong("id");
                    if (currentId > 0) {
                        Method setId = DataMapper.getMethodInHierarchy(instance.getClass(), "setId", Long.class);
                        if (setId == null) setId = DataMapper.getMethodInHierarchy(instance.getClass(), "setId", long.class);
                        if (setId != null) setId.invoke(instance, currentId);
                    }
                } catch (SQLException e) {
                    return;
                }
            }
            if (currentId <= 0) return;

            String parentTable = DataMapper.getTableName(superClass);
            String sqlParent = new QueryBuilder().select("*").from(parentTable).where("id = ?").build();
            try (PreparedStatement stmtP = conn.prepareStatement(sqlParent)) {
                stmtP.setLong(1, currentId);
                try (ResultSet rsParent = stmtP.executeQuery()) {
                    if (rsParent.next()) {
                        fillEntityRecursively(instance, superClass, rsParent, conn);
                    }
                }
            }
        }
    }

    private void readEntityComplete(IEntity entity, Connection conn) throws Exception {
        Class<?> clazz = entity.getClass();
        String tableName = DataMapper.getTableName(clazz);
        String sql = new QueryBuilder().select("*").from(tableName).where("id = ?").build();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, entity.getId());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    fillEntityRecursively(entity, clazz, rs, conn);
                }
            }
        }
    }

    @Override
    public Long update(T entity) {
        if (entity.getId() == null || entity.getId() <= 0) {
            throw new RuntimeException("ID inválido para atualização.");
        }
        try {
            Connection conn = UnitOfWork.getConnection();
            List<Class<?>> hierarchy = DataMapper.getEntityHierarchy(entity.getClass());
            for (Class<?> clazz : hierarchy) {
                updateForClass(conn, entity, clazz);
            }
            saveAssociations(conn, entity);
            return entity.getId();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar registro via Repositório: " + e.getMessage(), e);
        }
    }

    private void updateForClass(Connection conn, IEntity entity, Class<?> clazz) throws Exception {
        ArrayList<String> setClauses = new ArrayList<>();
        ArrayList<Object> values = new ArrayList<>();
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            if (DataMapper.isPersistentGetter(clazz, method)) {
                Object value = method.invoke(entity);
                if (value != null) {
                    boolean isEntity = value instanceof IEntity;
                    String colName = DataMapper.getColumnName(clazz, method, isEntity);
                    if (isEntity) {
                        value = ((IEntity) value).getId();
                    }
                    setClauses.add(colName + " = ?");
                    values.add(value);
                }
            }
        }

        if (setClauses.isEmpty()) return;

        String tableName = DataMapper.getTableName(clazz);
        String sql = new QueryBuilder().update(tableName).set(setClauses).where("id = ?").build();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int i = 1;
            for (Object val : values) {
                pstmt.setObject(i++, val);
            }
            pstmt.setLong(i, entity.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public Long delete(T entity) {
        if (entity.getId() == null || entity.getId() <= 0) {
            throw new RuntimeException("ID inválido para exclusão.");
        }
        try {
            Connection conn = UnitOfWork.getConnection();
            List<Class<?>> hierarchy = DataMapper.getEntityHierarchy(entity.getClass());
            Collections.reverse(hierarchy);
            for (Class<?> clazz : hierarchy) {
                String tableName = DataMapper.getTableName(clazz);
                String sql = new QueryBuilder().deleteFrom(tableName).where("id = ?").build();
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setLong(1, entity.getId());
                    pstmt.executeUpdate();
                }
            }
            return entity.getId();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao excluir registro via Repositório: " + e.getMessage(), e);
        }
    }

    private void saveAssociations(Connection conn, IEntity instance) throws Exception {
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (Collection.class.isAssignableFrom(field.getType())) {
                boolean isOneToMany = field.isAnnotationPresent(OneToMany.class);
                boolean isManyToMany = field.isAnnotationPresent(ManyToMany.class);
                if (!isOneToMany && !isManyToMany) continue;

                field.setAccessible(true);
                @SuppressWarnings("unchecked")
                Collection<?> collection = (Collection<?>) field.get(instance);
                if (collection == null || collection.isEmpty()) continue;

                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                Class<?> childClass = (Class<?>) listType.getActualTypeArguments()[0];

                for (Object item : collection) {
                    IEntity child = (IEntity) item;
                    if (isOneToMany) {
                        Method fkSetter = null;
                        String mappedBy = field.getAnnotation(OneToMany.class).mappedBy();
                        if (mappedBy != null && !mappedBy.trim().isEmpty()) {
                            String fkSetterName = "set" + DataMapper.capitalize(mappedBy);
                            fkSetter = DataMapper.getMethodInHierarchy(childClass, fkSetterName, instance.getClass());
                        }
                        if (fkSetter == null) {
                            String fkSetterName = "set" + instance.getClass().getSimpleName();
                            fkSetter = DataMapper.getMethodInHierarchy(childClass, fkSetterName, instance.getClass());
                        }
                        if (fkSetter != null) {
                            fkSetter.invoke(child, instance);
                            if (child.getId() == null || child.getId() <= 0) {
                                saveRecursiveInTransaction(conn, child);
                            }
                        }
                    } else if (isManyToMany) {
                        if (child.getId() == null || child.getId() <= 0) {
                            saveRecursiveInTransaction(conn, child);
                        }
                        String tableLink = DataMapper.getTableName(instance.getClass()) + "_" + DataMapper.getTableName(childClass);
                        String fkParent = DataMapper.convertPascalCaseToSnakeCase(instance.getClass().getSimpleName()) + "_id";
                        String fkChild = DataMapper.convertPascalCaseToSnakeCase(childClass.getSimpleName()) + "_id";

                        if (field.isAnnotationPresent(JoinTable.class)) {
                            JoinTable joinTable = field.getAnnotation(JoinTable.class);
                            if (!joinTable.name().isEmpty()) tableLink = joinTable.name();
                            if (joinTable.joinColumns().length > 0) fkParent = joinTable.joinColumns()[0].name();
                            if (joinTable.inverseJoinColumns().length > 0) fkChild = joinTable.inverseJoinColumns()[0].name();
                        }

                        String sqlM2M;
                        if (com.gwj.AppConfig.isPostgreSql()) {
                            sqlM2M = "INSERT INTO " + tableLink + " (" + fkParent + ", " + fkChild + ") VALUES (?, ?) ON CONFLICT DO NOTHING";
                        } else {
                            sqlM2M = "INSERT IGNORE INTO `" + tableLink + "` (`" + fkParent + "`, `" + fkChild + "`) VALUES (?, ?)";
                        }
                        try (PreparedStatement pstmt = conn.prepareStatement(sqlM2M)) {
                            pstmt.setLong(1, instance.getId());
                            pstmt.setLong(2, child.getId());
                            pstmt.executeUpdate();
                        }
                    }
                }
            }
        }
    }

    private Long saveRecursiveInTransaction(Connection conn, IEntity entity) throws Exception {
        List<Class<?>> hierarchy = DataMapper.getEntityHierarchy(entity.getClass());
        Long lastId = null;
        if (entity.getId() != null && entity.getId() > 0) return entity.getId();
        for (Class<?> clazz : hierarchy) {
            lastId = insertForClass(conn, entity, clazz, lastId);
        }
        entity.setId(lastId);
        saveAssociations(conn, entity);
        return lastId;
    }

    private void processAssociations(IEntity instance, Connection conn) throws Exception {
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (Collection.class.isAssignableFrom(field.getType())) {
                ParameterizedType listType = (ParameterizedType) field.getGenericType();
                Class<?> childClass = (Class<?>) listType.getActualTypeArguments()[0];

                if (IEntity.class.isAssignableFrom(childClass)) {
                    Method fkSetter = null;
                    if (field.isAnnotationPresent(OneToMany.class)) {
                        String mappedBy = field.getAnnotation(OneToMany.class).mappedBy();
                        if (mappedBy != null && !mappedBy.trim().isEmpty()) {
                            String fkSetterName = "set" + DataMapper.capitalize(mappedBy);
                            fkSetter = DataMapper.getMethodInHierarchy(childClass, fkSetterName, instance.getClass());
                        }
                    }
                    if (fkSetter == null) {
                        String fkSetterName = "set" + instance.getClass().getSimpleName();
                        fkSetter = DataMapper.getMethodInHierarchy(childClass, fkSetterName, instance.getClass());
                    }

                    if (fkSetter != null) {
                        IEntity criteria = (IEntity) childClass.getDeclaredConstructor().newInstance();
                        fkSetter.invoke(criteria, instance);
                        @SuppressWarnings("unchecked")
                        GenericRepository<IEntity> childRepo = new GenericRepository<>((Class<IEntity>) childClass);
                        List<IEntity> result = childRepo.query(criteria);
                        field.setAccessible(true);
                        field.set(instance, result);
                    } else {
                        try {
                            String sqlM2M = buildM2mSelectSql(instance.getClass(), childClass, field);
                            try (PreparedStatement stmt = conn.prepareStatement(sqlM2M)) {
                                stmt.setLong(1, instance.getId());
                                try (ResultSet rsM2M = stmt.executeQuery()) {
                                    List<IEntity> listaM2M = new ArrayList<>();
                                    while (rsM2M.next()) {
                                        IEntity child = (IEntity) childClass.getDeclaredConstructor().newInstance();
                                        fillEntityRecursively(child, childClass, rsM2M, conn);
                                        listaM2M.add(child);
                                    }
                                    field.setAccessible(true);
                                    field.set(instance, listaM2M);
                                }
                            }
                        } catch (SQLException e) {
                            System.err.println("Aviso: Falha ao associar N:N em repositório: " + e.getMessage());
                        }
                    }
                }
            }
        }
    }

    private String buildM2mSelectSql(Class<?> parentClass, Class<?> childClass, Field field) {
        String tableChild = DataMapper.getTableName(childClass);
        String tableLink = DataMapper.getTableName(parentClass) + "_" + DataMapper.getTableName(childClass);
        String fkChild = DataMapper.convertPascalCaseToSnakeCase(childClass.getSimpleName()) + "_id";
        String fkParent = DataMapper.convertPascalCaseToSnakeCase(parentClass.getSimpleName()) + "_id";

        if (field != null && field.isAnnotationPresent(JoinTable.class)) {
            JoinTable joinTable = field.getAnnotation(JoinTable.class);
            if (!joinTable.name().isEmpty()) tableLink = joinTable.name();
            if (joinTable.joinColumns().length > 0) fkParent = joinTable.joinColumns()[0].name();
            if (joinTable.inverseJoinColumns().length > 0) fkChild = joinTable.inverseJoinColumns()[0].name();
        }

        return new QueryBuilder()
                .select("c.*")
                .from(tableChild + " c")
                .innerJoin(tableLink + " l", "c.id = l." + fkChild)
                .where("l." + fkParent + " = ?")
                .build();
    }

    private String buildWhereClause(IEntity entity) {
        List<String> conditions = new ArrayList<>();
        if (entity.getId() != null && entity.getId() > 0) {
            conditions.add("id = " + entity.getId());
        }
        Class<?> clazz = entity.getClass();
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            if (DataMapper.isPersistentGetter(clazz, method)) {
                try {
                    Object value = method.invoke(entity);
                    if (isValidValue(value)) {
                        boolean isEntity = value instanceof IEntity;
                        String columnName = DataMapper.getColumnName(clazz, method, isEntity);
                        if (isEntity) {
                            value = ((IEntity) value).getId();
                        }
                        if (value instanceof String) {
                            conditions.add(columnName + " LIKE '%" + value + "%'");
                        } else if (value instanceof Number || value instanceof Boolean) {
                            conditions.add(columnName + " = " + value);
                        } else {
                            conditions.add(columnName + " = '" + value + "'");
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return String.join(" AND ", conditions);
    }

    private boolean isValidValue(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).doubleValue() > 0;
        if (value instanceof String) return !((String) value).trim().isEmpty();
        if (value instanceof Collection) return false;
        return true;
    }
}
