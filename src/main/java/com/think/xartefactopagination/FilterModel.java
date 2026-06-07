package com.think.xartefactopagination;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FilterModel {

    private String field;
    private String value;

    //@JsonIgnore
    private String operator = " AND ";

    @JsonIgnore
    private String columnTable;

    //@JsonIgnore
    private String type = "";    

}
