package com.think.commons;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SortModel {

    private String colName;
    private String direction;

    @JsonIgnore
    private String columnTable;

    @JsonIgnore
    private String type = "string";    

}
