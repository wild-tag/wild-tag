package management.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.wild_tag.model.ImageApi;
import com.wild_tag.model.ImagesBucketApi;
import java.util.List;
import management.entities.images.ImageContent;
import management.enums.UserRole.UserRoleNames;
import management.security.UserPrincipalParam;
import management.services.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/images")
@RequestMapping("/images")
public class ImagesController {

  @Autowired
  private ImageService imageService;

  @PostMapping("/upload")
  public ResponseEntity<Void> uploadImage(@RequestBody ImagesBucketApi imagesBucketApi) throws JsonProcessingException {
    imageService.loadImages(imagesBucketApi);
    return ResponseEntity.ok().build();
  }

  @GetMapping
  @Secured({UserRoleNames.ADMIN_ROLE, UserRoleNames.USER_ROLE})
  public ResponseEntity<List<ImageApi>> getImages(@UserPrincipalParam("email") String email) {
    return ResponseEntity.ok(imageService.getImages());
  }

  @PutMapping("/tag")
  @Secured({UserRoleNames.ADMIN_ROLE, UserRoleNames.USER_ROLE})
  public ResponseEntity<Void> tagImage(@RequestBody ImageApi imageApi, @UserPrincipalParam("email") String userEmail) {
    imageService.tagImage(imageApi, userEmail);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @PutMapping("{imageId}/validate")
  @Secured({UserRoleNames.ADMIN_ROLE, UserRoleNames.USER_ROLE})
  public ResponseEntity<Void> validateImage(@PathVariable String imageId,
      @UserPrincipalParam("email") String userEmail) {
    imageService.validateImage(imageId, userEmail);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  @GetMapping("{imageId}")
  @Secured({UserRoleNames.ADMIN_ROLE, UserRoleNames.USER_ROLE})
  public ResponseEntity<byte[]> getImage(@PathVariable String imageId) {
    ImageContent imageContent = imageService.getImageContent(imageId);
    return ResponseEntity.ok()
        .contentType(MediaType.valueOf(imageContent.getContentType()))
        .body(imageContent.getContent());
  }

  @GetMapping("next_task")
  @Secured({UserRoleNames.ADMIN_ROLE, UserRoleNames.USER_ROLE})
  public ResponseEntity<ImageApi> getNExtImageTask(@UserPrincipalParam("email") String email) {
    ImageApi nextTask =  imageService.getNextTask(email);
    return ResponseEntity.ok(nextTask);
  }
}
