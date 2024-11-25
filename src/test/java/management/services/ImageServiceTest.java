package management.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import management.entities.images.CoordinateDB;
import management.entities.images.ImageDB;
import management.entities.images.ImageStatus;
import management.entities.users.UserDB;
import management.repositories.ImagesRepository;
import management.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;

class ImageServiceTest {

  public static final String ROOT = "root";
  public static final String DATA_SET_BUCKET = "dataSetBucket";
  ImagesRepository imagesRepository = Mockito.mock(ImagesRepository.class);
  UserRepository userRepository = Mockito.mock(UserRepository.class);
  CloudStorageService cloudStorageService = Mockito.mock(CloudStorageService.class);


  ImageService imageService = new ImageService(cloudStorageService, imagesRepository,  userRepository);

  @BeforeEach
  public void setUp() {
    imageService.setDataSetBucket(DATA_SET_BUCKET);
    imageService.setStorageRootDir(ROOT);
    imageService.setValidateRate(50);
  }

//    coordinates.add(new CoordinateDB("1", 0.1, 0.5, 0.3, 0.2));
//    coordinates.add(new CoordinateDB("2", 0.3, 0.2, 0.1, 0.1));
  @Test
  void testBuildImageTag_singleObj() throws IOException {

    UserDB tagger = new UserDB();
    UserDB validator = new UserDB();
    List<CoordinateDB> coordinates = new ArrayList<>();
    coordinates.add(new CoordinateDB("1", 0.5, 0.5, 0.3, 0.2));
    ImageDB imageDb = new ImageDB("gs://bucket/path/to/yahmor.png", ImageStatus.TAGGED, tagger, validator, coordinates, null);

    Mockito.when(cloudStorageService.copyObject(eq(imageDb.getGcsFullPath()), eq(DATA_SET_BUCKET), eq("root/dataset/images/train/yahmor.png"))).thenReturn("GS://dataSetBucket/ROOT/dataset/images/train/yahmor.png");

    imageService.buildImageTag(imageDb);

    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);

    Mockito.verify(cloudStorageService, times(1)).uploadFileToStorage(
        eq(DATA_SET_BUCKET), eq("root/dataset/labels/train"), eq("yahmor.txt"),
        captor.capture()
    );
    assertTrue(captor.getValue().length > 10);
    Mockito.verify(cloudStorageService).copyObject(imageDb.getGcsFullPath(), DATA_SET_BUCKET, "root/dataset/images/train/yahmor.png");
    Mockito.verify(cloudStorageService, times(1)).deleteObject(imageDb.getGcsFullPath());
    ArgumentCaptor<ImageDB> imageCaptor = ArgumentCaptor.forClass(ImageDB.class);
    Mockito.verify(imagesRepository, times(1)).save(imageCaptor.capture());
    ImageDB savedImage = imageCaptor.getValue();

    assertEquals(ImageStatus.TRAINABLE, savedImage.getStatus());
    assertEquals(String.format("GS://%s/%s", DATA_SET_BUCKET, "ROOT/dataset/images/train/yahmor.png"), savedImage.getGcsTaggedPath());
  }

  @Test
  void testBuildImageTag_validate() throws IOException {

    UserDB tagger = new UserDB();
    UserDB validator = new UserDB();
    List<CoordinateDB> coordinates = new ArrayList<>();
    coordinates.add(new CoordinateDB("1", 0.5, 0.5, 0.3, 0.2));
    coordinates.add(new CoordinateDB("1", 0.1, 0.5, 0.3, 0.2));

    ImageDB imageDb = new ImageDB("gs://bucket/path/to/yahmor.png", ImageStatus.TAGGED, tagger, validator, coordinates, null);

    Mockito.when(cloudStorageService.copyObject(eq(imageDb.getGcsFullPath()), eq(DATA_SET_BUCKET), eq("root/dataset/images/val/yahmor.png"))).thenReturn("GS://dataSetBucket/ROOT/dataset/images/val/yahmor.png");

    imageService.buildImageTag(imageDb);

    ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);

    Mockito.verify(cloudStorageService, times(1)).uploadFileToStorage(
        eq(DATA_SET_BUCKET), eq("root/dataset/labels/val"), eq("yahmor.txt"),
        captor.capture()
    );
    assertTrue(captor.getValue().length > 10);
    Mockito.verify(cloudStorageService).copyObject(imageDb.getGcsFullPath(), DATA_SET_BUCKET, "root/dataset/images/val/yahmor.png");
    Mockito.verify(cloudStorageService, times(1)).deleteObject(imageDb.getGcsFullPath());
    ArgumentCaptor<ImageDB> imageCaptor = ArgumentCaptor.forClass(ImageDB.class);
    Mockito.verify(imagesRepository, times(1)).save(imageCaptor.capture());
    ImageDB savedImage = imageCaptor.getValue();

    assertEquals(ImageStatus.TRAINABLE, savedImage.getStatus());
    assertEquals(String.format("GS://%s/%s", DATA_SET_BUCKET, "ROOT/dataset/images/val/yahmor.png"), savedImage.getGcsTaggedPath());
  }
}