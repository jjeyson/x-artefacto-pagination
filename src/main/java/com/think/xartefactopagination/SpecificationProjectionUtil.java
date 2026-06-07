package com.think.xartefactopagination;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.panache.common.Page;

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
         Page pageable,
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
            if (sorts != null) {
                int i = 0;
                String direction = "ASC"; // Default direction
                for (var map : sorts.entrySet()) {
                    String key = map.getKey();

                    boolean keyMap = filtersMap.containsKey(key);
                    if (keyMap) {
                        if (i == 0)
                            orderByClause.append("ORDER BY ");

                        if (i > 0)
                            orderByClause.append(", ");

                        Map<String, Object> sortData = (Map<String, Object>) filtersMap.get(key);
                        String columnName = sortData.get("columnName").toString();
                        direction = map.getValue().toString().toUpperCase();
                        orderByClause.append(columnName).append(" ").append(direction);
                    }

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


        public static <R> R map(
                    Object[] row,
                    List<String> fields,
                    Class<R> dtoClass) {

                try {

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

                            setter.invoke(
                                    builder,
                                    value);
                        }
                    }

                    Method buildMethod = builder.getClass().getMethod("build");

                    return (R) buildMethod.invoke(builder);

                } catch (Exception e) {

                    throw new RuntimeException(e);

                }
    }            

}
