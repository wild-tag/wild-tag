package management.services;

import static management.entities.images.ImageStatus.VALIDATED;

import com.wild_tag.model.CoordinatesApi;
import com.wild_tag.model.ImageApi;
import com.wild_tag.model.ImageStatusApi;
import com.wild_tag.model.ImagesBucketApi;
import jakarta.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import management.entities.images.CoordinateDB;
import management.entities.images.ImageDB;
import management.entities.images.ImageStatus;
import management.entities.users.UserDB;
import management.repositories.ImagesRepository;
import management.repositories.UserRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class ImageService {

  public static final String DATASET = "dataset";
  public static final String LABELS = "labels";
  public static final String VAL = "val";
  public static final String TRAIN = "train";
  public static final String IMAGES = "images";
  private Logger logger = LoggerFactory.getLogger(ImageService.class);


  @Value("${data_set_bucket:wild-tag-data-set}")
  private String dataSetBucket;

  @Value("${storage_root_dir:dev}")
  private String storageRootDir;

  @Value("${validate_rate:15}")
  private Integer validateRate;

  private Map<String, Integer> categoriesHistogram;

  private CloudStorageService cloudStorageService;

  private ImagesRepository imagesRepository;

  private UserRepository usersRepository;

  public ImageService(CloudStorageService cloudStorageService, ImagesRepository imagesRepository, UserRepository usersRepository) {
    this.cloudStorageService = cloudStorageService;
    this.imagesRepository = imagesRepository;
    this.usersRepository = usersRepository;
  }

  public void loadImages(ImagesBucketApi imagesBucketApi) {
    Thread thread = new Thread(() -> loadImagesBackground(imagesBucketApi));
    thread.start();
  }

  public void loadImagesBackground(ImagesBucketApi imagesBucketApi) {
    logger.info("Loading images from bucket: {}", imagesBucketApi.getBucketName());
    List<String> images = cloudStorageService.listBucket(imagesBucketApi.getBucketName());
    images.forEach(imagePath -> {
      ImageDB imageDB = new ImageDB();
      imageDB.setGcsFullPath(imagePath);
      imagesRepository.save(imageDB);
    });
    logger.info(images.size() + " images loaded successfully");
  }

  public List<ImageApi> getImages() {
    return imagesRepository.findAll().stream().map(this::convertToImageApi).toList();
  }

  public void tagImage(ImageApi imageApi, String userEmail) {
    ImageDB imageDB = imagesRepository.findById(UUID.fromString(imageApi.getId())).orElseThrow();
    UserDB userDB = usersRepository.findByEmail(userEmail).orElseThrow();
    imageDB.setTaggerUser(userDB);
    imageDB.setStatus(ImageStatus.TAGGED);
    imageDB.setCoordinates(imageApi.getCoordinates().stream().map(this::convertCoordinatesApiToCoordinatesDB).toList());
    imagesRepository.save(imageDB);
  }

  public void validateImage(String imageId, String userEmail) {
    ImageDB imageDB = imagesRepository.findById(UUID.fromString(imageId)).orElseThrow();
    UserDB validatorUser = usersRepository.findByEmail(userEmail).orElseThrow();
    imageDB.setValidatorUser(validatorUser);
    imageDB.setStatus(VALIDATED);
    imagesRepository.save(imageDB);
  }

  private ImageApi convertToImageApi(ImageDB imageDB) {
    return new ImageApi().id(imageDB.getId().toString()).
        status(ImageStatusApi.fromValue(imageDB.getStatus().name())).
        coordinates(imageDB.getCoordinates().stream().map(this::convertCoordinatesDBToCoordinatesApi).toList()).
        validatorUserId(imageDB.getValidatorUser() == null ? "unassigned" : imageDB.getValidatorUser().getEmail()).
        taggerUserId(imageDB.getTaggerUser() == null ? "unassigned" : imageDB.getTaggerUser().getEmail());

  }

  private CoordinatesApi convertCoordinatesDBToCoordinatesApi(CoordinateDB coordinates) {
    return new CoordinatesApi().animalId(coordinates.getAnimalId()).
        xCenter(coordinates.getX_center()).
        yCenter(coordinates.getY_center()).
        width(coordinates.getWidth()).
        height(coordinates.getHeight());
  }

  private CoordinateDB convertCoordinatesApiToCoordinatesDB(CoordinatesApi coordinatesApi) {
    return new CoordinateDB().setAnimalId(coordinatesApi.getAnimalId()).
        setX_center(coordinatesApi.getxCenter()).
        setY_center(coordinatesApi.getyCenter()).
        setWidth(coordinatesApi.getWidth()).
        setHeight(coordinatesApi.getHeight());
  }

  public List<ImageDB> getValidatedImages(int limit) {
    return imagesRepository.getByStatus(ImageStatus.VALIDATED, PageRequest.of(0, limit));
  }

  @Transactional
  public void buildImageTag(ImageDB imageDB) throws IOException {
    String yoloImagePath = createYoloFiles(imageDB);
    imageDB.setGcsTaggedPath(yoloImagePath);
    imageDB.setStatus(ImageStatus.TRAINABLE);
    imagesRepository.save(imageDB);
  }

  public String createYoloFiles(ImageDB imageDB) throws IOException {
    List<CoordinateDB> coordinates = imageDB.getCoordinates(); // Assuming a getter for coordinates exists

    // Create a byte array output stream to temporarily hold the file content
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

      byte[] yoloTextByteArray = getYoloText(coordinates, writer, outputStream);

      boolean isValidate = isValidate(imageDB);

      String[] parts = imageDB.getGcsFullPath().split("/");
      String objectName = parts[parts.length - 1];

      uploadTextFile(isValidate, convertToTxtFileName(objectName), yoloTextByteArray);
      return moveImageToDataSetPath(isValidate, imageDB.getGcsFullPath(), objectName);
    } catch (IOException e) {
      logger.error("failed to create yolo files for image {}", imageDB.getId(), e);
      throw e;
    }
  }

  public static String convertToTxtFileName(String imageFileName) {

    int dotIndex = imageFileName.lastIndexOf('.');
    if (dotIndex == -1) {
      return imageFileName + ".txt"; // No extension found, append .txt
    } else {
      return imageFileName.substring(0, dotIndex) + ".txt"; // Replace existing extension
    }
  }


  private String uploadTextFile(boolean isValidate, String objectName, byte[] yoloTextByteArray) {
    String folder = String.format("%s/%s/%s/%s", storageRootDir, DATASET, LABELS, isValidate ? VAL : TRAIN);
    return cloudStorageService.uploadFileToStorage(dataSetBucket, folder, objectName, yoloTextByteArray);
  }

  private String moveImageToDataSetPath(boolean isValidate, String gcsFullPath, String objectName) {

    String destinationObject = String.format("%s/%s/%s/%s/%s", storageRootDir, DATASET, IMAGES, isValidate ? VAL : TRAIN, objectName);
    cloudStorageService.copyObject(gcsFullPath, dataSetBucket, destinationObject);
    String copiedImageUri = String.format("gs://%s/%s", dataSetBucket, destinationObject);
    cloudStorageService.deleteObject(gcsFullPath);
    return copiedImageUri;
  }

  private boolean isValidate(ImageDB imageDB) {
    boolean isValidate = false;

    for (CoordinateDB coordinateDB : imageDB.getCoordinates()) {
      String category = coordinateDB.getAnimalId();
      int categoryObjects = categoriesHistogram.get(category) != null ? categoriesHistogram.get(category) : 0;
      categoriesHistogram.put(category, ++categoryObjects);
      if (categoryObjects % validateRate == 0) {
        isValidate = true;
      }
    }

    return isValidate;
  }

  @NotNull
  private static byte[] getYoloText(List<CoordinateDB> coordinates, Writer writer, ByteArrayOutputStream outputStream)
      throws IOException {
    for (CoordinateDB coord : coordinates) {
      int classId = Integer.parseInt(coord.getAnimalId());
      double xCenter = coord.getX_center();
      double yCenter = coord.getY_center();
      double width = coord.getWidth();
      double height = coord.getHeight();

      // Format the line in YOLO format: class x_center y_center width height
      String line = String.format("%d %.2f %.2f %.2f %.2f", classId, xCenter, yCenter, width, height);
      writer.write(line);
      writer.write("\n");
    }
    writer.flush();

    return outputStream.toByteArray();
  }

}
