package applications;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import management.entities.images.CoordinateDB;
import management.entities.images.ImageDB;
import management.entities.images.ImageStatus;
import management.repositories.ImagesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "management")
@EnableJpaRepositories(basePackages = "management.repositories")
@EntityScan(basePackages = "management.entities")
public class Tagger implements CommandLineRunner {


  @Autowired ImagesRepository imagesRepository;
  private int limit;

  public static void main(String[] args) {
    SpringApplication.run(Tagger.class, args);
  }

  @Override
  public void run(String... args) throws Exception {

    while (true) {
      List<ImageDB> images = imagesRepository.getByStatus(ImageStatus.VALIDATED, PageRequest.of(0, limit));
      if (images.isEmpty()) {
        return;
      }

      for (ImageDB image : images) {
        //build yolo text

        //write the file to location

        //move the image

        //set the new path in the db object

        //set new status
      }
    }
  }

  public static void createYoloFile(ImageDB imageDB, String filePath) {
    List<CoordinateDB> coordinates = imageDB.getJson(); // Assuming a getter for coordinates exists

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
      for (CoordinateDB coordinate : coordinates) {
        int classId = Integer.parseInt(coordinate.getId());
        double xCenter = coordinate.getX_center();
        double yCenter = coordinate.getY_center();
        double width = coordinate.getWidth();
        double height = coordinate.getHeight();

        // Format the line in YOLO format: class x_center y_center width height
        String line = String.format("%d %.2f %.2f %.2f %.2f", classId, xCenter, yCenter, width, height);
        writer.write(line);
        writer.newLine();
      }
    } catch (Exception e) {
      e.printStackTrace(); // Handle the exception as needed
    }
  }

}
