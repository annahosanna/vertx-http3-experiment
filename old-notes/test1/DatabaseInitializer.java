package example;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

  public static void initDb() throws Exception {
    try (Connection conn = DatabaseConnection.getConnection()) {
      Statement stmt = conn.createStatement();

      stmt.execute(
        "CREATE TABLE IF NOT EXISTS fortunes (id INT PRIMARY KEY AUTO_INCREMENT, text VARCHAR(255))"
      );

      stmt.execute(
        "INSERT INTO fortunes (text) VALUES ('Fortune favors the bold')"
      );
      stmt.execute(
        "INSERT INTO fortunes (text) VALUES ('A journey of a thousand miles begins with a single step')"
      );
      stmt.execute(
        "INSERT INTO fortunes (text) VALUES ('The best time to plant a tree was 20 years ago. The second best time is now')"
      );
      stmt.execute(
        "INSERT INTO fortunes (text) VALUES ('Be the change you wish to see in the world')"
      );
      stmt.execute(
        "INSERT INTO fortunes (text) VALUES ('Today is the first day of the rest of your life')"
      );
    }
  }
}
