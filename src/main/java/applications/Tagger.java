package applications;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
import management.entities.images.CoordinateDB;
import management.entities.images.ImageDB;
import management.entities.images.ImageStatus;
import management.repositories.ImagesRepository;
import management.services.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private Logger logger = LoggerFactory.getLogger(Tagger.class);

  @Autowired ImageService imageService;
  private int limit;

  public static void main(String[] args) {
    SpringApplication.run(Tagger.class, args);
  }

  @Override
  public void run(String... args) throws Exception {

    while (true) {
      List<ImageDB> images = imageService.getValidatedImages(limit);
      if (images.isEmpty()) {
        return;
      }

      for (ImageDB image : images) {
        try {
          imageService.buildImageTag(image);
        }
        catch (Exception e) {
          logger.error("failed to tag image {}", image.getId(), e);
        }
      }
    }
  }
}
