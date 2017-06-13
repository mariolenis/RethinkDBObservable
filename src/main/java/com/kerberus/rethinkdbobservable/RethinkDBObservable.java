package com.kerberus.rethinkdbobservable;
/**
 *
 * @author Jorge Mario Lenis
 */
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.BehaviorSubject;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class RethinkDBObservable<T extends RethinkDBObject> {
    
    private final BehaviorSubject<ArrayList<T>> db$;
    private final String API_URL;
    private final RethinkDBAPIConfig config;
    private final Class parseableClass;
    
    private Observable<ArrayList<T>> queryObservable$;
    
    // TODO: Get clasd from Generic T
    public RethinkDBObservable( 
            RethinkDBAPIConfig config, 
            BehaviorSubject<RethinkDBQuery> query$, 
            Class parseableClass
    ) {
        
        this.config         = config;
        this.parseableClass = parseableClass;
        this.db$            = BehaviorSubject.createDefault(new ArrayList<>());
        this.API_URL        = (config.host != null ? config.host : "")  + (config.port > -1 ? ":" + config.port : "");
        
        try {
            // Creates a namespace to listen events and populate db$ with new data triggered by filter observable            
            Socket socket = IO.socket(API_URL);
            
            queryObservable$ = Observable.just(socket.connect())
                
                // Validate the connection
                .flatMap(sck -> validateConnectionCredentials(sck))
                    
                // Start the listener from backend, also if gets disconnected and reconnected, emits message to refresh the query
                .flatMap(nsp -> listenFromBackend(nsp))

                // If query$ has next value, will trigger a new query without modifying the subscription filter in backend
                .flatMap(noinfo -> (query$ != null ? query$ : Observable.just(new RethinkDBQuery(-1, null, null))))
                    
                // Register the change's listener
                .switchMap(query -> registerListener(socket, query))
                    
                // Executes the query 
                .switchMap(query -> queryDB(query));
            
        } catch (URISyntaxException ex) {
            Logger.getLogger(RethinkDBObservable.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    //<editor-fold defaultstate="collapsed" desc="private Observable<Socket> validateConnectionCredentials(Socket socket)">
    private Observable<Socket> validateConnectionCredentials(Socket socket) {
        return Observable.create(o -> {
            
            Map localConfig = new HashMap();
            localConfig.put("db", config.database);
            localConfig.put("table", config.table);
            localConfig.put("api_key", config.api_key);
            
            socket.emit("validate", JSON.parseMapToString(localConfig), new Ack() {
                @Override
                public void call(Object... args) {
                    if (String.valueOf(args[args.length - 1]).contains("err"))
                        o.onError(new Exception("Unauthorized api_key " + localConfig.get("api_key")));
                    else
                        o.onNext(socket);
                }
            });
            
        });
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="private Observable<String> listenFromBackend(Socket nsp)">
    private Observable<String> listenFromBackend(Socket nsp) {
        
        return Observable.create(o -> {
            nsp.on(config.table, new Emitter.Listener() {
                @Override
                public void call(Object... os) {
                    
                    String predata = String.valueOf(os[os.length - 1]);
                    Map<String, T> data = JSON.<T>parseStringToGenericMap(predata, parseableClass);
                    
                    // Current state
                    ArrayList<T> db = new ArrayList<>(db$.getValue());
                    
                    // New data
                    if (data.get("old_val") == null && data.get("new_val") != null) {
                        db.add(data.get("new_val"));
                        db$.onNext(db);
                    }
                    
                    // Update data
                    else if (data.get("old_val") != null && data.get("new_val") != null) {
                        db.removeIf(object -> object.id.equals(data.get("old_val").id));
                        db.add(data.get("new_val"));
                        db$.onNext(db);
                    }
                    
                    // Delete data
                    else if (data.get("old_val") != null && data.get("new_val") == null) {
                        db.removeIf(object -> object.id.equals(data.get("old_val").id));
                        db$.onNext(db);
                    }
                }
            });
            
            nsp.on("reconnect", (Object... os) -> {
                o.onNext("Reconnect");
            });
            
            o.onNext("Start");            
        });
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="private Observable<RethinkDBQuery> registerListener(Socket socket, RethinkDBQuery query)">
    private Observable<RethinkDBQuery> registerListener(Socket socket, RethinkDBQuery query) {
        return Observable.create(obs -> {
            Map<String, String> changeConfig = new HashMap<>();
            changeConfig.put("db", config.database);
            changeConfig.put("table", config.table);
            changeConfig.put("query", query.toString());
            
            socket.emit("listenChanges", JSON.parseMapToString(changeConfig));
            obs.onNext(query);
            obs.onComplete();
        });
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="private Observable<ArrayList<T>> queryDB(RethinkDBQuery query)">
    private Observable<ArrayList<T>> queryDB(RethinkDBQuery query) {
        return Observable.just(query)
            .map(_query -> {
                Map body = new HashMap();
                body.put("db", config.database);
                body.put("table", config.table);
                body.put("api_key", config.api_key);
                body.put("query", _query.toString());
                return body;
            })
            .flatMap(body -> httpRequest(API_URL + "/api/list", body))
            .map(response -> {
                InputStream is = response.getEntity().getContent();
                String predata = IOUtils.toString(is, "UTF-8");
                return JSON.<T>parseStringToGenericArrayList(predata, parseableClass);
            });
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="public Observable<String> push(T object)">
    public Observable<String> push(T object) {
        
        return Observable.just(object)
            .map(_object -> {
                Map body = new HashMap();
                body.put("db", config.database);
                body.put("table", config.table);
                body.put("api_key", config.api_key);
                body.put("object", _object.toString());
                return body;
            })
            .flatMap(body -> httpRequest(API_URL + "/api/put", body))
            .map(response -> {
                InputStream is = response.getEntity().getContent();
                return IOUtils.toString(is, "UTF-8");
            });
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="public Observable<String> update(T object)">
    public Observable<String> update(T object) {
        return Observable.just(object)
            .map(_object -> {
                Map body = new HashMap();
                body.put("db", config.database);
                body.put("table", config.table);
                body.put("api_key", config.api_key);
                body.put("object", _object.toString());
                return body;
            })
            .flatMap(body -> httpRequest(API_URL + "/api/update", body))
            .map(response -> {
                InputStream is = response.getEntity().getContent();
                return IOUtils.toString(is, "UTF-8");
            });
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="public Observable<String> update(T object, RethinkDBQuery query)">
    public Observable<String> update(T object, RethinkDBQuery query) {
        return Observable.just(object)
            .map(_object -> {
                Map body = new HashMap();
                body.put("db", config.database);
                body.put("table", config.table);
                body.put("api_key", config.api_key);
                body.put("object", _object.toString());
                body.put("query", query.toString());
                return body;
            })
            .flatMap(body -> httpRequest(API_URL + "/api/update", body))
            .map(response -> {
                InputStream is = response.getEntity().getContent();
                return IOUtils.toString(is, "UTF-8");
            });
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="public Observable<String> remove(String index)">
    public Observable<String> remove(String index) {
        return Observable.just(index)
            .map(indexValue -> {
                Map body = new HashMap();
                body.put("db", config.database);
                body.put("table", config.table);
                body.put("api_key", config.api_key);
                body.put("query", "{\"id\":\""+ indexValue +"\"}");
                return body;
            })
            .flatMap(body -> httpRequest(API_URL + "/api/delete", body))
            .map(response -> {
                InputStream is = response.getEntity().getContent();
                return IOUtils.toString(is, "UTF-8");
            });
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="public Observable<String> remove(String index, String indexValue)">
    public Observable<String> remove(String index, String indexValue) {
        Map<String, String> indexMap = new HashMap<>();
        indexMap.put(index, indexValue);
        
        return Observable.just(indexMap)
            .map(_map -> {
                Map body = new HashMap();
                body.put("db", config.database);
                body.put("table", config.table);
                body.put("api_key", config.api_key);
                body.put("query", JSON.parseMapToString(_map));
                return body;
            })
            .flatMap(body -> httpRequest(API_URL + "/api/delete", body))
            .map(response -> {
                InputStream is = response.getEntity().getContent();
                return IOUtils.toString(is, "UTF-8");
            });
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="private Observable<String> httpRequest(String URL, Map body)">
    private Observable<CloseableHttpResponse> httpRequest(String URL, Map body) {
        return Observable.create(obs -> {
            try {
                CloseableHttpClient client = HttpClients.createDefault();
                HttpPost httpPost = new HttpPost(URL);
                
                httpPost.setHeader("Accept", "application/json, text/plain, */*");
                httpPost.setHeader("Content-type", "application/json");
                
                StringEntity params = new StringEntity(JSON.parseMapToString(body));
                httpPost.setEntity(params);                
                
                obs.onNext(client.execute(httpPost));                
                client.close();
                obs.onComplete();
                
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(RethinkDBObservable.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(RethinkDBObservable.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }
    //</editor-fold>
        
    //<editor-fold defaultstate="collapsed" desc="public Disposable subscribe(onNext, onError, onComplete)">    
    /**
     * 
     * @param onNext
     * @return Disposable
     */
    public Disposable subscribe(Consumer<? super ArrayList<T>> onNext) {
        queryObservable$.subscribe(result -> db$.onNext(result));
        return db$.subscribe(onNext);
    }
    /**
     * 
     * @param onNext
     * @param onError
     * @return Disposable
     */
    public Disposable subscribe(Consumer<? super ArrayList<T>> onNext, Consumer<? super Throwable> onError) {
        queryObservable$.subscribe(result -> db$.onNext(result));
        return db$.subscribe(onNext, onError);
    }
    /**
     * 
     * @param onNext
     * @param onError
     * @param onComplete
     * @return Disposable
     */
    public Disposable subscribe(Consumer<? super ArrayList<T>> onNext, Consumer<? super Throwable> onError, Action onComplete) {
        queryObservable$.subscribe(result -> db$.onNext(result));
        return db$.subscribe(onNext, onError, onComplete);
    }
    //</editor-fold>
}