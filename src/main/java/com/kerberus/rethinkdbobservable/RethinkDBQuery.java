package com.kerberus.rethinkdbobservable;

import java.util.Map;

/**
 *
 * @author kerberus
 */
public class RethinkDBQuery {
    int limit;
    String orderBy;
    Map<String, String> filter;

    public RethinkDBQuery(int limit, String orderBy, Map<String, String> filter) {
        this.filter = filter;
        this.orderBy = orderBy;
        this.limit = limit;
    }
}
