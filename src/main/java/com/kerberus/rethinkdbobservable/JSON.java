package com.kerberus.rethinkdbobservable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase de funciones estáticas que dan soporte a Mapas en JSON.
 * @author kerberus
 */
public class JSON {
    /**
     * Función para parsear un objeto a partir de una cadena
     * @param jsonString String
     * @param parseClase Class.name()
     * @return Object
     */
    //<editor-fold defaultstate="collapsed" desc="static Object parseJSONStringToObject(String jsonString, Class parseClase)">
    public static Object parseJSONStringToObject(String jsonString, Class parseClase) {
        Gson gson = new GsonBuilder().create();        
        return gson.fromJson(jsonString, parseClase);
    }
    //</editor-fold>
    
    /**
     * Función para convertir una cadena en un objeto T
     * @param <T>
     * @param jsonString
     * @return Map<String, T>
     */
    //<editor-fold defaultstate="collapsed" desc="static <T> T parseStringToGenericMap(String jsonString)">
    public static <T> Map<String, T> parseStringToGenericMap(String jsonString, Class parseClass) {
        Gson gson = new GsonBuilder().create();
        
        Map<String, LinkedTreeMap> map = gson.fromJson(jsonString, new TypeToken<Map<String, LinkedTreeMap>>(){}.getType());
        Map<String, T> finalMap = new HashMap<>();

        map.forEach((key, jsonThree) -> {
            
            if (jsonThree != null) {
                Map<String, Object> _map = new HashMap<>();
                jsonThree.forEach((k, v) -> {
                    _map.put(String.valueOf(k), v);
                });
                finalMap.put(key, (T) gson.fromJson( gson.toJson(_map), parseClass) );
            } 
            else
                finalMap.put(key, null);
        });
        
        return finalMap;
    }
    //</editor-fold>
    
    /**
     * Función para convertir una cadena en un ArrayList de T
     * @param <T>
     * @param jsonString
     * @return 
     */
    public static <T> ArrayList<T> parseStringToGenericArrayList(String jsonString, Class parseClass) {
        Gson gson = new GsonBuilder().create();
        ArrayList<LinkedTreeMap> threeArray = gson.fromJson(jsonString, new TypeToken<ArrayList<LinkedTreeMap>>(){}.getType());
        ArrayList<T> finalArray = new ArrayList<>();
        
        threeArray.forEach(jsonThree -> {
            
            Map<String, Object> _map = new HashMap<>();
            jsonThree.forEach((key, value) -> {
                _map.put(String.valueOf(key), value);
            });
            
            finalArray.add((T)gson.fromJson( gson.toJson(_map) , parseClass));
        });
        return finalArray;
    }
    
    /**
     * Función para parsear una cadena a Map
     * @param jsonString
     * @return Map<String, String>
     */
    //<editor-fold defaultstate="collapsed" desc="static Map<String, String> parseStringToMap(String jsonString)">
    public static Map<String, String> parseStringToMap(String jsonString){
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(jsonString, (new TypeToken<Map<String, String>>(){}).getType());
    }
    //</editor-fold>
    
    /**
     * Función para parsear una cadena a una lista de mapas
     * @param jsonString String
     * @return List Map
     */
    //<editor-fold defaultstate="collapsed" desc="static <T> List<Map<String, T>> parseStringToListMap(String jsonString)">
    public static <T> List<Map<String, T>> parseStringToListMap(String jsonString){
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(jsonString, (new TypeToken<List<Map<String, T>>>(){}).getType());
    }
    //</editor-fold>
    
    /**
     * Función para parsear un mapa a cadena
     * @param mapa Map
     * @return Stfring
     */
    //<editor-fold defaultstate="collapsed" desc="static String parseMapToString(Map mapa)">
    public static String parseMapToString(Map mapa) {
        Gson gson = new GsonBuilder().create();
        Type listType = new TypeToken<Map<String, String>>(){}.getType();
        return gson.toJson(mapa, listType);
    }
    //</editor-fold>
    
    /**
     * Función para parsear una lista
     * @param  V, T extends List<Map<String, V>> Lista
     * @return String JSON
     */
    //<editor-fold defaultstate="collapsed" desc="static <V, T extends List<Map<String, V>>> String parseListMapToString(T lista)">
    public static <V, T extends List<Map<String, V>>> String parseListMapToString(T userList) {
        Gson gson = new GsonBuilder().create();        
        return gson.toJson(userList, new TypeToken<T>(){}.getType());
    }
    //</editor-fold>
}
