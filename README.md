# RethinkDBObservable
RethinkDB Observable class for realtime database in java

## Usage
Here is a simple demo
### Module 1st
In order to use this, you will need to build a backend seervice for RethinkDB, __or__ you might use this one 
https://github.com/mariolenis/rethinkdb-daas

```java
package com.kerberus.rethinkdbobservable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.Map;


public class Test {
    
    public static void main(String[] args) {
        RethinkDBAPIConfig config = new RethinkDBAPIConfig();
        
        config.api_key = "AAAA-BBBB-CCCC-DDDD";
        config.database = "<your-database>";
        config.table = "<your-table>";
        config.host = "http://<your-ip>";
        config.port = <your-port>;
        
        RethinkDBObservable<Message> message = new RethinkDBObservable(config, null, Message.class);
        message.subscribe(msg -> {
            System.out.println("msg = " + msg.toString());
            if (msg.size() > 3)
                System.out.println("msg.get(2) = " + msg.get(2).toString());
        });
    }
}


class Message extends RethinkDBObject {
    String name, msg, date;
    
    public Message() {
        super();
    }
    
    @Override
    public String toString() {
        
        Map<String, String> message = new HashMap<>();
        message.put("id", id);
        message.put("name", name);
        message.put("msg", msg);
        message.put("date", date);
        
        Gson gson = new GsonBuilder().create();
        return gson.toJson(message);
    }
}
```
