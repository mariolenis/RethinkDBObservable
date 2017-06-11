package com.kerberus.rethinkdbobservable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
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
     * Función statica que toma un ResulSet y retorna una lista de Objetos
     * @param resultSet ResultSet
     * @param claseObjeto Class.class
     * @return ArrayList Object
     * @throws SQLException
     * @throws ClassNotFoundException 
     */
    //<editor-fold defaultstate="collapsed" desc="static ArrayList parseResultSetToObjectList(ResultSet resultSet, Class clase) throws SQLException, ClassNotFoundException">
    public static ArrayList parseResultSetToObjectList(ResultSet resultSet, Class claseObjeto) throws SQLException, ClassNotFoundException{
        
        Gson gson = new GsonBuilder().create();
        ResultSetMetaData meta = resultSet.getMetaData();
        ArrayList<Object> object = new ArrayList<>();
        
        while(resultSet.next()) {
            Map<String, Object> mapaPropiedades = new HashMap<>();
            for(int i=1; i <= meta.getColumnCount(); i++) {
                mapaPropiedades.put(meta.getColumnName(i), resultSet.getObject(i));
            }
            String jsonStringObject = gson.toJson(mapaPropiedades);
            object.add(gson.fromJson(jsonStringObject, claseObjeto));
        }
        return object;
    }
    //</editor-fold>
    
    /**
     * Función statica que toma un ResulSet y retorna una lista de mapas
     * @param resultSet ResultSet
     * @return ArrayList Map
     * @throws SQLException
     * @throws ClassNotFoundException 
     */
    //<editor-fold defaultstate="collapsed" desc="static ArrayList parseResultSetToMapList(ResultSet resultSet) throws SQLException, ClassNotFoundException">
    public static ArrayList parseResultSetToMapList(ResultSet resultSet) throws SQLException, ClassNotFoundException{
        
        ResultSetMetaData meta = resultSet.getMetaData();
        ArrayList<Map<String, String>> object = new ArrayList<>();
        
        while(resultSet.next()) {
            Map<String, String> mapa = new HashMap<>();
            for(int i=1; i <= meta.getColumnCount(); i++) {
                mapa.put(meta.getColumnName(i), resultSet.getString(i));
            }
            object.add(mapa);
        }
        return object;
    }
    //</editor-fold>
    
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
     * Función para parsear una cadena a Map
     * @param jsonString
     * @return MAP String,String
     */
    //<editor-fold defaultstate="collapsed" desc="static Map<String, String> parseStringToMap(String jsonString)">
    public static Map<String, String> parseStringToMap(String jsonString){
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(jsonString, (new TypeToken<Map<String, Object>>(){}).getType());
    }
    //</editor-fold>
    
    /**
     * Función para parsear una cadena a Map
     * @param jsonString
     * @param typeMap 
     * @return MAP String,String
     */
    //<editor-fold defaultstate="collapsed" desc="static Map<String, String> parseStringToMap(String jsonString)">
    public static Map<String, String> parseStringToMap(String jsonString, Type typeMap){
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(jsonString, typeMap);
    }
    //</editor-fold>
    
    /**
     * Función para parsear una cadena a una lista de mapas
     * @param jsonString String
     * @return List Map
     */
    //<editor-fold defaultstate="collapsed" desc="static List<Map<String, String>> parseStringToListMap(String jsonString)">
    public static List<Map<String, String>> parseStringToListMap(String jsonString){
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(jsonString, (new TypeToken<List<Map<String, String>>>(){}).getType());
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
        Type listType = new TypeToken<Map<String, Object>>(){}.getType();
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
