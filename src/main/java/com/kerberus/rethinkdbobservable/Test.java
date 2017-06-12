package com.kerberus.rethinkdbobservable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author kerberus
 */
public class Test {
    
    public static void main(String[] args) {
        RethinkDBAPIConfig config = new RethinkDBAPIConfig();
        
        config.api_key = "AAAA-BBBB-CCCC-DDDD";
        config.database = "flownter";
        config.table = "counter";
        config.host = "http://localhost";
        config.port = 3200;
        
        RethinkDBObservable<Mensaje> message = new RethinkDBObservable(config, null, Mensaje.class);
        message.subscribe(msg -> {
            System.out.println("msg = " + msg.toString());
            if (msg.size() > 3)
                System.out.println("msg.get(2) = " + msg.get(2).toString());
        });
        
        try {
            Thread.sleep(3000);
            System.out.println("End of Thread " + Thread.currentThread().getName());
        } catch (InterruptedException ex) {
            Logger.getLogger(Test.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}


class Mensaje extends RethinkDBObject {
    String nombre, msg, date;
    
    public Mensaje() {
        super();
    }
    
    @Override
    public String toString() {
        
        Map<String, String> message = new HashMap<>();
        message.put("id", id);
        message.put("nombre", nombre);
        message.put("msg", msg);
        message.put("date", date);
        
        Gson gson = new GsonBuilder().create();
        return gson.toJson(message);
    }
}