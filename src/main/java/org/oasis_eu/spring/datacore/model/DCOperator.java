package org.oasis_eu.spring.datacore.model;

/**
 * User: schambon
 * Date: 1/3/14
 */
public enum DCOperator {

    EMPTY(""),
    EQ(""),
    GT(">"),
    GTE(">="),
    LT("<"),
    LTE("<="),
    NE("<>"),
    IN("$in"),
    NIN("$nin"),
    REGEX("$regex"),
    EXISTS("$exists"),
    FULLTEXT("$fulltext");

    private final String representation;

    private DCOperator(String representation) {
        this.representation = representation;
    }


    public String getRepresentation() {
        return representation;
    }
}
