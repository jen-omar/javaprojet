package tn.esprit.mythoria.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {
    private String URL="jdbc:mysql://localhost:3306/mythoria_db";
    private String USER="root";
    private String PASSWORD="";
    private Connection connection;
    private static MyDatabase myDatabase;
    private MyDatabase(){
        try {
            connection= DriverManager.getConnection(URL,USER,PASSWORD);
            System.out.println("Connexion etablie avec succes");
        } catch (SQLException e) {
            System.out.println("Erreur de connexion "+ e.getMessage());
        }
    }
    public static MyDatabase getInstance(){
        if(myDatabase==null){
            myDatabase=new MyDatabase();
        }else{
            System.out.println("Connexion deja etablie");
        }
        return myDatabase;
    }
    public Connection getConnection(){
        return connection;
    }
}
