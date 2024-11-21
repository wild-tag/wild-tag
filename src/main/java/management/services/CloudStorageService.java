package management.services;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.google.cloud.storage.Storage;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CloudStorageService {

  private final Storage storageClient;

  public CloudStorageService() throws IOException {
    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
    this.storageClient = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
  }


  public List<String> listBucket(String bucketName) {
    List<String> files = new ArrayList<>();
    storageClient
        .list(bucketName)
        .iterateAll()
        .forEach(blob -> files.add("gs://" + blob.getBucket() + "/" + blob.getName()));
    return files;
  }
}
