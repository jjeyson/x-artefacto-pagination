package com.think.xartefactopagination;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaginationModel {

    private Integer pageNumber;
    private Integer rowsPerPage;

    private List<FilterModel> filters;
    private List<SortModel> sorts;

    @JsonIgnore
    private Map<String, Object> filtersMap;

    @JsonIgnore
    private Map<String, Object> sortMap;

    @JsonIgnore
    public Map<String, Object> getParamsWhere() {
        return this.getFilters().stream()
                .collect(Collectors.toMap(FilterModel::getField, FilterModel::getValue));

    }

    @JsonIgnore
    public Map<String, String> getParamsOrderBy() {
        return this.getSorts().stream()
                .collect(Collectors.toMap(SortModel::getColName, SortModel::getDirection));

    }

    @JsonIgnore
    public Map<String, Object> getFiltersMap() {
        filtersMap = new HashMap<>();

        if (this.filters != null) {

            filtersMap = this.getFilters().stream()
                    .collect(Collectors.toMap(
                            FilterModel::getField,
                            filter -> {
                                Map<String, Object> map = new HashMap<>();
                                map.put("value", filter.getValue());
                                map.put("operator", filter.getOperator());
                                map.put("columnName", "p." + filter.getField());
                                map.put("type", filter.getType());
                                return map;
                            },
                            (existing, replacement) -> existing
                        ));
        }

        return filtersMap;
    }

    @JsonIgnore
    public Map<String, Object> getSortMap() {
        sortMap = new HashMap<>();
        if (this.sorts != null) {
            sortMap = this.getSorts().stream()
                    .collect(Collectors.toMap(
                            sort -> sort.getColName().toString(),
                            sort -> sort.getDirection()));
        }

        return sortMap;
    }

}
