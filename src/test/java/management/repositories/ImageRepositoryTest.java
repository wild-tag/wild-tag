package management.repositories;

import applications.Application;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import management.controllers.NATSTestSimulator;
import management.entities.images.CoordinateDB;
import management.entities.images.ImageDB;
import management.entities.images.ImageStatus;
import management.entities.users.UserDB;
import management.enums.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@SpringBootTest(classes = Application.class)
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class ImageRepositoryTest extends DbTestSimulator {

  @Autowired
  UserRepository userRepository;

  @Autowired
  ImagesRepository imageRepository;

  @Test
  public void t() {
    UserDB u1 = userRepository.save(new UserDB("name", "abc@google.com", UserRole.ADMIN));
    UserDB u2 = userRepository.save(new UserDB("name", "efg@google.com", UserRole.ADMIN));
    CoordinateDB coordinateDB = new CoordinateDB("1", 0.67, 0.5, 0.7, 0.3);
    ImageDB image = new ImageDB("gcsPath", ImageStatus.PENDING, u1, u2, Collections.singletonList(coordinateDB), "taggedPath");
    image = imageRepository.save(image);

    Optional<ImageDB> imageOptional = imageRepository.findById(image.getId());
    ImageDB imageDB = imageOptional.orElseThrow();
    Assertions.assertEquals("gcsPath", imageDB.getGcsFullPath());
    Assertions.assertEquals("taggedPath", imageDB.getGcsTaggedPath());
    Assertions.assertEquals(u1.getEmail(), imageDB.getTaggerUser().getEmail());
  }

  @Test
  public void testGetNextTask_toValidate() {

    UserDB u1 = userRepository.save(new UserDB("name", "abc@google.com", UserRole.ADMIN));
    UserDB u2 = userRepository.save(new UserDB("name", "efg@google.com", UserRole.ADMIN));

    ImageDB imageDB = new ImageDB();
    imageDB.setStatus(ImageStatus.TAGGED);
    imageDB.setTaggerUser(u2);
    imageDB.setStartHandled(Timestamp.valueOf("1970-01-01 00:00:00"));

    ImageDB imageDB2 = new ImageDB();
    imageDB2.setStatus(ImageStatus.PENDING);
    imageDB2.setStartHandled(Timestamp.valueOf("1970-01-01 00:00:00"));

    imageRepository.save(imageDB);
    imageRepository.save(imageDB2);


    Pageable pageable = PageRequest.of(0, 1);
    List<ImageDB> result = imageRepository.getNextTask(ImageStatus.PENDING, ImageStatus.TAGGED, u1,
        Timestamp.valueOf("1970-01-01 00:00:01"), pageable);

    Assertions.assertEquals(1, result.size());
    ImageDB fetched = result.get(0);
    Assertions.assertEquals(ImageStatus.TAGGED ,fetched.getStatus());
  }

  @Test
  public void testGetNextTask_byUser() {

    UserDB u1 = userRepository.save(new UserDB("name", "abc@google.com", UserRole.ADMIN));
    UserDB u2 = userRepository.save(new UserDB("name", "efg@google.com", UserRole.ADMIN));

    ImageDB imageDB = new ImageDB();
    imageDB.setStatus(ImageStatus.TAGGED);
    imageDB.setTaggerUser(u2);
    imageDB.setStartHandled(Timestamp.valueOf("1970-01-01 00:00:00"));

    ImageDB imageDB2 = new ImageDB();
    imageDB2.setStatus(ImageStatus.PENDING);
    imageDB2.setStartHandled(Timestamp.valueOf("1970-01-01 00:00:00"));

    imageRepository.save(imageDB);
    imageRepository.save(imageDB2);


    Pageable pageable = PageRequest.of(0, 1);
    List<ImageDB> result = imageRepository.getNextTask(ImageStatus.PENDING, ImageStatus.TAGGED, u2,
        Timestamp.valueOf("1970-01-01 00:00:01"), pageable);

    Assertions.assertEquals(1, result.size());
    ImageDB fetched = result.get(0);
    Assertions.assertEquals(ImageStatus.PENDING ,fetched.getStatus());
  }

  @Test
  public void testGetNextTask_byTime() {

    UserDB u1 = userRepository.save(new UserDB("name", "abc@google.com", UserRole.ADMIN));
    UserDB u2 = userRepository.save(new UserDB("name", "efg@google.com", UserRole.ADMIN));

    ImageDB imageDB = new ImageDB();
    imageDB.setStatus(ImageStatus.TAGGED);
    imageDB.setTaggerUser(u1);
    imageDB.setStartHandled(Timestamp.valueOf("1970-01-01 00:00:02"));

    ImageDB imageDB2 = new ImageDB();
    imageDB2.setStatus(ImageStatus.PENDING);
    imageDB2.setStartHandled(Timestamp.valueOf("1970-01-01 00:00:00"));

    imageRepository.save(imageDB);
    imageRepository.save(imageDB2);


    Pageable pageable = PageRequest.of(0, 1);
    List<ImageDB> result = imageRepository.getNextTask(ImageStatus.PENDING, ImageStatus.TAGGED, u2,
        Timestamp.valueOf("1970-01-01 00:00:01"), pageable);

    Assertions.assertEquals(1, result.size());
    ImageDB fetched = result.get(0);
    Assertions.assertEquals(ImageStatus.PENDING ,fetched.getStatus());
  }
}
