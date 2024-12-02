package management.repositories;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import management.controllers.NATSTestSimulator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;


public class DbTestSimulator extends NATSTestSimulator {

  static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15")
      .withDatabaseName("test")
      .withUsername("test")
      .withPassword("test");

  @DynamicPropertySource
  static void postgresqlProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
    registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
    registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
  }

  @BeforeAll
  public static void startContainer() {
    NATSTestSimulator.startContainer();
    postgreSQLContainer.start();
  }


  @AfterAll
  public static void stopContainer() {
    NATSTestSimulator.stopContainer();
    postgreSQLContainer.stop();
  }

  @AfterEach
  public void clearDatabase() {
    try (Connection connection = postgreSQLContainer.createConnection("")) {
      Statement stmt = connection.createStatement();
      ResultSet rs = stmt.executeQuery(
          "SELECT tablename FROM pg_tables WHERE schemaname='public'");
      List<String> tables = new ArrayList<>();
      while (rs.next()) {
        tables.add(rs.getString(1));
      }
      for (String table : tables) {
        stmt.execute("TRUNCATE TABLE " + table + " CASCADE");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
