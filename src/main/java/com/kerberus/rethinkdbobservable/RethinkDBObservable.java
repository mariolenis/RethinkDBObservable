package com.kerberus.rethinkdbobservable;
/**
 *
 * @author Jorge Mario Lenis
 */
import io.reactivex.Observable;
import io.reactivex.Observer;
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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

public class RethinkDBObservable<T extends RethinkDBObject> {
    
    private final BehaviorSubject<ArrayList<T>> db$;
    private final String API_URL;
    private final RethinkDBAPIConfig config;
    private final String table;
    
    public RethinkDBObservable( RethinkDBAPIConfig config, String table, BehaviorSubject<RethinkDBQuery> query$ ) {
        this.config     = config;
        this.table      = table;
        this.db$        = BehaviorSubject.createDefault(new ArrayList<>());
        this.API_URL    = (config.host != null ? config.host : "")  + (config.port > -1 ? ":" + config.port : "");
        
        try {            
            Socket socket = IO.socket(API_URL);
            initSocketIO(socket)
                .flatMap(nsp -> listenFromBackend(nsp))
                .flatMap(msg -> (query$ != null ? query$ : Observable.just(null)))
                .switchMap(query -> registerListener(socket, query))
                .switchMap(query -> queryDB(query))
                .subscribe(
                        data -> System.out.println("data = " + data),
                        err -> System.err.println(err)
                );
            
        } catch (URISyntaxException ex) {
            Logger.getLogger(RethinkDBObservable.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private Observable<Socket> initSocketIO(Socket socket) {
        
        return new Observable<Socket>() {
            @Override
            protected void subscribeActual(Observer<? super Socket> o) {
                Map localConfig = new HashMap();
                localConfig.put("db", config.database);
                localConfig.put("table", table);
                localConfig.put("api_key", config.api_key);
                
                socket.emit("join", JSON.parseMapToString(localConfig), new Ack() {
                    @Override
                    public void call(Object... args) {
                        if (String.valueOf(args[args.length - 1]).contains("err"))
                            o.onError(new Throwable("Unauthorized api_key"));
                        else
                            o.onNext(socket);
                        o.onComplete();
                    }
                });
                o.onNext(socket);
                o.onComplete();
            }
        };
    }
    
    private Observable<String> listenFromBackend(Socket nsp) {
        return new Observable<String>() {
            @Override
            protected void subscribeActual(Observer<? super String> o) {
                
                nsp.on("reconnect", new Emitter.Listener() {
                    @Override
                    public void call(Object... os) {
                        o.onNext(String.valueOf(os[os.length - 1]));
                    }
                });
                
                nsp.on(table, new Emitter.Listener() {
                    @Override
                    public void call(Object... os) {
                        
                        String predata = String.valueOf(os[os.length - 1]);
                        System.out.println("predata = " + predata);
                        // Current state
                        ArrayList<T> db = db$.getValue();                        
                    }
                });
            }
        };
    }
    
    private Observable<RethinkDBQuery> registerListener(Socket socket, RethinkDBQuery query) {
        return new Observable<RethinkDBQuery>() {
            @Override
            protected void subscribeActual(Observer<? super RethinkDBQuery> obs) {
                Map<String, String> changeConfig = new HashMap<>();
                changeConfig.put("db", config.database);
                changeConfig.put("table", table);
                changeConfig.put("query", query.toString());
                socket.emit("listenChanges", JSON.parseMapToString(changeConfig));
                obs.onNext(query);
                obs.onComplete();
            }
        };
    }
    
    private Observable<ArrayList<T>> queryDB (RethinkDBQuery query) {
        return new Observable<ArrayList<T>>() {
            @Override
            protected void subscribeActual(Observer<? super ArrayList<T>> obs) {
                try {
                    CloseableHttpClient client = HttpClients.createDefault();
                    HttpPost httpPost = new HttpPost(API_URL + "/api/list");
                    
                    httpPost.setHeader("Accept", "application/json, text/plain, */*");
                    httpPost.setHeader("Content-type", "application/json");
                    
                    List<NameValuePair> params = new ArrayList<>();
                    params.add(new BasicNameValuePair("db", config.database));
                    params.add(new BasicNameValuePair("table", table));
                    params.add(new BasicNameValuePair("api_key", config.api_key));
                    params.add(new BasicNameValuePair("query", query.toString()));
                    
                    httpPost.setEntity(new UrlEncodedFormEntity(params));
                    
                    CloseableHttpResponse response = client.execute(httpPost);
                    InputStream is = response.getEntity().getContent();
                    String predata = IOUtils.toString(is, "UTF-8"); 
                    System.out.println("predata = " + predata);
                    
                    // TODO: Transform predata into ArrayList<T>
                    // obs.onNext();
                    client.close();
                    obs.onComplete();
                } catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(RethinkDBObservable.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(RethinkDBObservable.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };
    }
}
