package com.gwj.model.dataAccessObject;

import com.gwj.model.domain.IEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DataMapper {
    private static final String tablePrefix = com.gwj.AppConfig.TABLE_PREFIX;

    public static List<Class<?>> getEntityHierarchy(Class<?> startClass) {
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = startClass;
        while (current != null && IEntity.class.isAssignableFrom(current) && current != Object.class) {
            hierarchy.add(0, current);
            current = current.getSuperclass();
        }
        return hierarchy;
    }

    public static String getTableName(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Table.class)) {
            String name = clazz.getAnnotation(Table.class).name();
            if (name != null && !name.isEmpty()) return name;
        }
        return tablePrefix + convertPascalCaseToSnakeCase(clazz.getSimpleName());
    }

    public static String convertPascalCaseToSnakeCase(String name) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('_');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }

    public static String getColumnName(Class<?> clazz, Method method, boolean isEntityValue) {
        String methodName = method.getName();
        String propName = methodName.startsWith("is") ? methodName.substring(2) : methodName.substring(3);
        String fieldName = propName.substring(0, 1).toLowerCase() + propName.substring(1);

        Field field = getFieldInHierarchy(clazz, fieldName);
        if (field != null) {
            if (field.isAnnotationPresent(JoinColumn.class)) {
                String name = field.getAnnotation(JoinColumn.class).name();
                if (name != null && !name.isEmpty()) return name;
            }
            if (field.isAnnotationPresent(Column.class)) {
                String name = field.getAnnotation(Column.class).name();
                if (name != null && !name.isEmpty()) return name;
            }
        }
        String colName = convertPascalCaseToSnakeCase(propName);
        if (isEntityValue) {
            colName += "_id";
        }
        return colName;
    }

    public static Field getFieldInHierarchy(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    public static Method getMethodInHierarchy(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    public static boolean isSetter(Method method) {
        return method.getName().startsWith("set") && method.getParameterCount() == 1 && Modifier.isPublic(method.getModifiers());
    }

    public static boolean isGetter(Method method) {
        String name = method.getName();
        return (name.startsWith("get") || name.startsWith("is"))
                && method.getParameterCount() == 0
                && !name.equals("getClass")
                && Modifier.isPublic(method.getModifiers());
    }

    public static boolean isPersistentGetter(Class<?> clazz, Method method) {
        if (!isGetter(method)) return false;
        if (Collection.class.isAssignableFrom(method.getReturnType())) return false;
        if (method.getName().equalsIgnoreCase("getId")) return false;
        if (method.isAnnotationPresent(jakarta.persistence.Transient.class)) return false;

        String propName = method.getName().startsWith("is") ? method.getName().substring(2) : method.getName().substring(3);
        if (propName.isEmpty()) return false;
        String fieldName = propName.substring(0, 1).toLowerCase() + propName.substring(1);
        Field field = getFieldInHierarchy(clazz, fieldName);
        if (field == null) return false;
        if (field.isAnnotationPresent(jakarta.persistence.Transient.class) || Modifier.isTransient(field.getModifiers())) return false;

        return true;
    }

    @SuppressWarnings("unchecked")
    public static Object convertToTargetType(Object value, Class<?> targetType, Field field) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;
        
        if (targetType.isEnum()) {
            if (field != null && field.isAnnotationPresent(Enumerated.class)) {
                EnumType enumType = field.getAnnotation(Enumerated.class).value();
                if (enumType == EnumType.ORDINAL) {
                    int ordinal = (value instanceof Number) ? ((Number) value).intValue() : Integer.parseInt(value.toString());
                    return targetType.getEnumConstants()[ordinal];
                }
            }
            return Enum.valueOf((Class<Enum>) targetType, value.toString().toUpperCase().trim());
        }

        return switch (targetType.getSimpleName()) {
            case "Integer", "int" -> switch (value) {
                case Number n -> n.intValue();
                case String s -> Integer.parseInt(s.trim());
                default -> 0;
            };
            case "Long", "long" -> {
                if (value instanceof Number n) yield n.longValue();
                yield 0L;
            }
            case "BigDecimal" -> new java.math.BigDecimal(value.toString());
            case "Boolean", "boolean" -> switch (value) {
                case Boolean b -> b;
                case Number n -> n.intValue() != 0;
                case String s -> s.equalsIgnoreCase("true") || s.equals("1");
                default -> false;
            };
            case "Date" -> (value instanceof java.util.Date d) ? new java.util.Date(d.getTime()) : 0;
            case "LocalDateTime" -> switch (value) {
                case java.sql.Timestamp t -> t.toLocalDateTime();
                case java.sql.Date d -> d.toLocalDate().atStartOfDay();
                case String s -> java.time.LocalDateTime.parse(s);
                default -> 0;
            };
            case "LocalDate" -> switch (value) {
                case java.sql.Date d -> d.toLocalDate();
                case java.sql.Timestamp t -> t.toLocalDateTime().toLocalDate();
                case String s -> java.time.LocalDate.parse(s);
                default -> value;
            };
            case "LocalTime" -> switch (value) {
                case java.sql.Time t -> t.toLocalTime();
                case java.sql.Timestamp ts -> ts.toLocalDateTime().toLocalTime();
                case String s -> java.time.LocalTime.parse(s);
                default -> value;
            };
            default -> value;
        };
    }
    
    public static String capitalize(String str) {
        return (str == null || str.length() == 0) ? str
            : str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
