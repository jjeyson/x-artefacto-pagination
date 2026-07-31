package com.think.xartefactopagination;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public final class SpecificationProjectionUtil {

    private SpecificationProjectionUtil() {
    }

    public static <T, R> PageResponse<R> filterSortProjection(
         EntityManager entityManager,
         Class<T> entityClass,
         String selectClause ,
         String joinClause,
         List<String> fieldsReturnResultClass,
         Map<String, Object> filtersMap,
         Map<String, Object> sorts,
         PageableCommons pageable,
         Class<R> resultClass
         ) {

            filtersMap = filtersMap == null
                    ? Collections.emptyMap()
                    : filtersMap;

            sorts = sorts == null
                    ? Collections.emptyMap()
                    : sorts;     
                    
                    


            StringBuilder whereClause = new StringBuilder();
            StringBuilder orderByClause = new StringBuilder();

            whereClause = buildWhereClause( filtersMap);
            orderByClause = buildOrderByClause(sorts, filtersMap);


            String jpql = selectClause  + "  "
                    + (joinClause != null ? joinClause + " " : "")
                    + (whereClause != null ? whereClause + " " : "")
                    + (orderByClause != null ? orderByClause + " " : "");



            TypedQuery<?> selectQuery = (TypedQuery) entityManager.createQuery(jpql);

            selectQuery = (TypedQuery<R>) setQueryParams(filtersMap, selectQuery);


            
            String countJpql = "SELECT COUNT(p) FROM " + entityClass.getSimpleName() + " p "
                        + (joinClause != null ? joinClause + " " : "")
                        + whereClause;

            selectQuery = (TypedQuery<R>) setQueryParams(filtersMap, selectQuery);

            TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);   
            
            countQuery = (TypedQuery<Long>) setQueryParams(filtersMap, countQuery);

            Long totalElements = countQuery.getSingleResult();
            int pageSize = pageable.size > 0 ? pageable.size : 1;
            int totalPages = (int) Math.ceil((double) totalElements / pageSize);
            selectQuery.setFirstResult(pageable.index * pageSize);
            selectQuery.setMaxResults(pageSize);
            List<Object[]> result = (List<Object[]>) selectQuery.getResultList();


            List<R> content = result.stream()
                .map(row -> map(row, fieldsReturnResultClass, resultClass))
                .toList();       

            return new PageResponse<>(content, pageable.index, pageSize, totalElements, totalPages);

         }


        static StringBuilder buildWhereClause( Map<String, Object> filtersMap) {
            StringBuilder whereClause = new StringBuilder();

                    if (filtersMap != null && !filtersMap.isEmpty()) {
                        whereClause.append("WHERE ");
                        int i = 0;

                        for (String key : filtersMap.keySet()) {

                            if (i > 0)
                                whereClause.append(" AND ");

                            // el key buscar en filtersMap si existe, si existe usar el columnName, si no
                            // existe usar el key
                            boolean keyMap = filtersMap != null && filtersMap.containsKey(key);

                            Object subMap = filtersMap.get(key);
                            Object keyColumnName    = subMap instanceof Map ? ((Map<?, ?>) subMap).get("columnName") : null;
                            Object keyOperator      = subMap instanceof Map ? ((Map<?, ?>) subMap).get("operator") : null;
                     


                            if (keyMap) {


                                String operatorKey = keyOperator instanceof EnumsOperators
                                        ? ((EnumsOperators) keyOperator).name()
                                        : String.valueOf(keyOperator);
                                String operatorText = keyOperator instanceof EnumsOperators
                                        ? ((EnumsOperators) keyOperator).getOperator()
                                        : String.valueOf(keyOperator);
                                String operatoSimbolo = EnumsOperators.getOperatorByName(operatorKey);

                                switch (operatorKey) {


                                    case "LIKE":
                                         whereClause.append("LOWER(").append(keyColumnName).append(") LIKE :").append(key.replace(".", "_"));
                                         break;
        
                                    case "BETWEEN":
                                        // Para BETWEEN, asumimos que el valor es una lista o array con dos elementos: [valorInicio, valorFin]
                                        whereClause.append(keyColumnName).append(" ")
                                            .append(operatorText).append(" :")
                                            .append(key.replace(".", "_")).append("Start AND :")
                                            .append(key.replace(".", "_")).append("End");
                                        break;
                                    case "DATE_RANGE":
                                        // Para DATE_RANGE, asumimos que el valor es una lista o array con dos elementos: [fechaInicio, fechaFin]
                                        whereClause
                                            .append(keyColumnName).append(" >= :")
                                            .append(key.replace(".", "_")).append("1")
                                            .append(" AND ")
                                            .append(keyColumnName).append(" <= :")
                                            .append(key.replace(".", "_")).append("2");
                                        break;

                                    default:
                                        whereClause.append(keyColumnName).append(" ").append(operatoSimbolo).append(" :")
                                                .append(key.replace(".", "_"));
                                        break;
                                }
                            } else {
                                whereClause.append(key).append(" = :").append(key.replace(".", "_"));
                            }

                            i++;

                        }

                    }
                    return whereClause;
         
         }

         static  StringBuilder buildOrderByClause(Map<String, Object> sorts, Map<String, Object> filtersMap) {

            StringBuilder orderByClause = new StringBuilder();
            if (sorts != null && !sorts.isEmpty()) {
                int i = 0;
                for (var map : sorts.entrySet()) {
                    String key = map.getKey();
                    String direction = map.getValue() != null ? map.getValue().toString().toUpperCase() : "ASC";

                    if (i == 0) {
                        orderByClause.append("ORDER BY ");
                    } else {
                        orderByClause.append(", ");
                    }

                    String columnName = key;
                    if (filtersMap != null && filtersMap.containsKey(key)) {
                        Object sortData = filtersMap.get(key);
                        if (sortData instanceof Map<?, ?> sortMap) {
                            Object columnValue = sortMap.get("columnName");
                            if (columnValue != null) {
                                columnName = columnValue.toString();
                            }
                        }
                    }

                    orderByClause.append(columnName).append(" ").append(direction);
                    i++;
                }
            }
            return orderByClause;
         }


        static TypedQuery<?> setQueryParams(  Map<String, Object> filtersMap, TypedQuery<?> query) {
            StringBuilder whereClause = new StringBuilder();

                    if (filtersMap != null && !filtersMap.isEmpty()) {
                        whereClause.append("WHERE ");
                        int i = 0;

                        for (String key : filtersMap.keySet()) {

                            if (i > 0)
                                whereClause.append(" AND ");

                            // el key buscar en filtersMap si existe, si existe usar el columnName, si no
                            // existe usar el key
                            boolean keyMap = filtersMap != null && filtersMap.containsKey(key);

                            Object subMap = filtersMap.get(key);
                            Object keyOperator      = subMap instanceof Map ? ((Map<?, ?>) subMap).get("operator") : null;

                            if (keyMap) {

                                String operatorKey = keyOperator instanceof EnumsOperators
                                        ? ((EnumsOperators) keyOperator).name()
                                        : String.valueOf(keyOperator);

                                Map<String, Object> datos = (Map<String, Object>) filtersMap.get(key);
                                String value = (String)datos.get("value");
                                String type = (String)datos.get("type");                                        

                                switch (operatorKey) {
                                    case "LIKE":
                                        query.setParameter(key.replace(".", "_"), "%" + value + "%");
                                        break;
                                    case "BETWEEN":
                                        query.setParameter(key.replace(".", "_") + "Start", ((List<?>) filtersMap.get(key)).get(0));
                                        query.setParameter(key.replace(".", "_") + "End", ((List<?>) filtersMap.get(key)).get(1));
                                        break;
                                    case "DATE_RANGE":
                                        String[] fechas = value.split("\\|");
                                        LocalDate fechaInicio = LocalDate.parse(fechas[0]);
                                        LocalDate fechaFin = LocalDate.parse(fechas[1]);
                                        LocalDateTime fechaInicioDateTime = fechaInicio.atStartOfDay();
                                        LocalDateTime fechaFinDateTime = fechaFin.atTime(23, 59, 59);

                                        query.setParameter(key.replace(".", "_")+"1" ,  fechaInicioDateTime);
                                        query.setParameter(key.replace(".", "_")+"2" ,  fechaFinDateTime);
                                        break;
                                    default:
                                        switch (type){
                                            case "text":
                                                query.setParameter(key.replace(".", "_"), value);
                                                break;
                                            case "number":
                                                query.setParameter(key.replace(".", "_"), Long.valueOf(value));
                                                break;
                                            case "decimal":
                                                query.setParameter(key.replace(".", "_"), Double.valueOf(value));
                                                break;
                                            case "boolean":
                                                query.setParameter(key.replace(".", "_"), Boolean.valueOf(value));
                                                break;

                                            default:
                                                query.setParameter(key.replace(".", "_"), datos.get("value"));
                                                break;
                                        }    
                                }
                            } else {
                                query.setParameter(key.replace(".", "_"), filtersMap.get(key));
                            }

                            i++;

                        }

                    }
                    return query;
         
         }


        private static Object convertValue(Object value, Class<?> targetType) {
            if (value == null || targetType == null || targetType.isInstance(value)) {
                return value;
            }

            if (targetType == String.class) {
                if (value instanceof Date date) {
                    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
                }
                if (value instanceof LocalDate localDate) {
                    return localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
                if (value instanceof LocalDateTime localDateTime) {
                    return localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
                return String.valueOf(value);
            }

            if (targetType == Long.class || targetType == long.class) {
                if (value instanceof Number number) {
                    return number.longValue();
                }
                return Long.valueOf(value.toString());
            }

            if (targetType == Integer.class || targetType == int.class) {
                if (value instanceof Number number) {
                    return number.intValue();
                }
                return Integer.valueOf(value.toString());
            }

            if (targetType == Double.class || targetType == double.class) {
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
                return Double.valueOf(value.toString());
            }

            if (targetType == Boolean.class || targetType == boolean.class) {
                if (value instanceof Boolean bool) {
                    return bool;
                }
                return Boolean.valueOf(value.toString());
            }

            if (targetType == LocalDate.class) {
                if (value instanceof LocalDate localDate) {
                    return localDate;
                }
                if (value instanceof Date date) {
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
                if (value instanceof String text) {
                    return LocalDate.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                }
            }

            if (targetType == LocalDateTime.class) {
                if (value instanceof LocalDateTime localDateTime) {
                    return localDateTime;
                }
                if (value instanceof Date date) {
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
                if (value instanceof String text) {
                    try {
                        return LocalDateTime.parse(text);
                    } catch (Exception ignored) {
                        return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    }
                }
            }

            if (targetType.isEnum() && value instanceof String text) {
                return Enum.valueOf((Class<? extends Enum>) targetType, text);
            }

            return value;
        }

        public static <R> R map(
                    Object[] row,
                    List<String> fields,
                    Class<R> dtoClass) {

                try {
                    if (dtoClass.isRecord()) {
                        Constructor<?> ctor = dtoClass.getDeclaredConstructors()[0];
                        @SuppressWarnings("unchecked")
                        Constructor<R> constructor = (Constructor<R>) ctor;
                        constructor.setAccessible(true);

                        Class<?>[] parameterTypes = constructor.getParameterTypes();
                        Object[] values = new Object[parameterTypes.length];

                        if(parameterTypes.length > fields.size()){
                            throw new IllegalArgumentException("El número de campos en fields no coincide con el número de parámetros en el constructor del DTO");
                        }

                        for (int i = 0; i < parameterTypes.length; i++) {
                            if (i < fields.size()) {

                                Object convertedValue =  convertValue(row[i], parameterTypes[i]);
                                values[i] = convertedValue;
                                  
                                //values[i] = row[i];
                                //System.out.println("Mapping field: " + fields.get(i) + " with value: " + values[i]);
                            } else {
                                values[i] = null;
                            }
                        }
                        //System.out.println("Mapping row to DTO: " + Arrays.toString(values));
                        return constructor.newInstance(values);
                    }

                    Method builderMethod = dtoClass.getMethod("builder");
                    Object builder = builderMethod.invoke(null);

                    for (int i = 0; i < fields.size(); i++) {

                        String fieldName = fields.get(i);
                        Object value = row[i];

                        if (value == null) {
                            continue;
                        }

                        Method setter =
                                Arrays.stream(
                                        builder.getClass()
                                                .getMethods())
                                        .filter(m ->
                                                m.getName()
                                                .equals(fieldName))
                                        .findFirst()
                                        .orElse(null);

                        if (setter != null) {
                            Class<?>[] paramTypes = setter.getParameterTypes();
                            Object convertedValue = paramTypes.length > 0
                                    ? convertValue(value, paramTypes[0])
                                    : value;
                            setter.invoke(builder, convertedValue);
                        }
                    }

                    Method buildMethod = builder.getClass().getMethod("build");
                    return (R) buildMethod.invoke(builder);

                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error mapping row to DTO: " + e.getMessage());
                    
                    throw new RuntimeException(e);
                }
    }            

}
