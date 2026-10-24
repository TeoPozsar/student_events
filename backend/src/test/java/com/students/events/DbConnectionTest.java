
package com.students.events;

import java.sql.Connection;
import java.sql.DriverManager;

public class DbConnectionTest {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:postgresql://127.0.0.1:5433/student_events";
        String user = "student_user";
        String password = "change_me_local_dev";

        try (Connection connection =
                     DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected successfully!");
            System.out.println("Database: " +
                    connection.getMetaData().getDatabaseProductName());
            System.out.println("User: " +
                    connection.getMetaData().getUserName());
        }
    }
}
