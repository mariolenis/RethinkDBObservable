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
        
        // Initialize your object from table, if does not exists, it will be created
        // new (RethinkDBAPIConfig config, BehaviorSubject<IRethinkDBQuery> query$, MyClass.class)
        RethinkDBObservable<Message> message = new RethinkDBObservable(config, null, Message.class);

        // Subscribe to your object and listen to data
        message
            .subscribe(msg -> {
                System.out.println("msg.size() = " + msg.size());
                if (msg.size() > 0)
                    System.out.println("msg.get(0) = " + msg.get(0).toString());
            });

        // Push new Data
        Message myMsg = new Message();
        myMsg.name = "Myself";
        myMsg.msg  = "Hi from RethinkDBObservable ReactiveX";
        myMsg.date = (new Date()).toString();
        message.put(myMsg)
            .subscribe(System.out::println);
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
