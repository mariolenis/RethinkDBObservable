package com.kerberus.rethinkdbobservable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
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
    
    @Override
    public String toString() {
        Map<String, String> query = new HashMap<>();
        if (limit > -1)
            query.put("limit", String.valueOf(limit));
        if (orderBy != null)
            query.put("orderBy", orderBy);
        if (filter != null)
            query.put("filter", JSON.parseMapToString(filter));
        
        Gson gson = new GsonBuilder().create();
        return gson.toJson(query);
    }
}
