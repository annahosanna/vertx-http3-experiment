package example;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseConnection {

  private static final String URL = "jdbc:h2:mem:fortunes;DB_CLOSE_DELAY=-1";

  public static Connection getConnection() throws Exception {
    return DriverManager.getConnection(URL);
  }
}
