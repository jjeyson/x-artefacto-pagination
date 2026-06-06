package com.think.commons;

public enum EnumsOperators {

    EQUAL("="),
    EQUALS("="),
    NOT_EQUAL("!="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_THAN_EQUAL(">="),
    GREATER_OR_EQUALS(">="),
    LESS_THAN_EQUAL("<="),
    LESS_OR_EQUALS("<="),
    LIKE("LIKE"),
    NOT_LIKE("NOT LIKE"),
    IN("IN"),
    NOT_IN("NOT IN"),
    BETWEEN("BETWEEN"),
    IS_NULL("IS NULL"),
    IS_NOT_NULL("IS NOT NULL"),
    AND("AND"),
    OR("OR"),
    NOT("NOT"),
    EXISTS("EXISTS"),
    NOT_EXISTS("NOT EXISTS"),
    STARTS_WITH("STARTS_WITH"),
    ENDS_WITH("ENDS_WITH"),
    CONTAINS("CONTAINS"),
    ILIKE("ILIKE"),
    DATE_RANGE("DATE_RANGE");

    
    private final String operator;

    EnumsOperators(String operator) {
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    public static EnumsOperators findByName(String name) {
        if (name == null) {
            return null;
        }

        for (EnumsOperators operator : values()) {
            if (operator.name().equalsIgnoreCase(name)) {
                return operator;
            }
        }

        return null;
    }

    public static String getOperatorByName(String name) {
        EnumsOperators operator = findByName(name);
        return operator == null ? null : operator.getOperator();
    }

}
