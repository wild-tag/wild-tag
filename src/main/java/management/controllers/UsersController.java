package management.controllers;

import com.wild_tag.model.UserApi;
import java.util.List;
import management.enums.UserRole.UserRoleNames;
import management.security.UserPrincipalParam;
import management.services.UsersService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsersController {

  private final UsersService usersService;

  public UsersController(UsersService usersService) {
    this.usersService = usersService;
  }

  @PostMapping("/login")
  public ResponseEntity<UserApi> login(@UserPrincipalParam("email") String email) {
    UserApi userDB = usersService.getUserByEmailByUserApi(email);
    return new ResponseEntity<>(userDB, HttpStatus.OK);
  }

  @Secured({UserRoleNames.ADMIN_ROLE})
  @GetMapping("/users")
  public ResponseEntity<List<UserApi>> getUsers() {
    List<UserApi> users = usersService.getUsers();
    return new ResponseEntity<>(users, HttpStatus.OK);
  }

  @Secured({UserRoleNames.ADMIN_ROLE})
  @GetMapping("/users/{userEmail}")
  public ResponseEntity<UserApi> getUserByEmail(@PathVariable String userEmail) {
    UserApi user = usersService.getUserByEmailByUserApi(userEmail);
    return new ResponseEntity<>(user, HttpStatus.OK);
  }

  @Secured({UserRoleNames.ADMIN_ROLE})
  @PostMapping("/users")
  public ResponseEntity<UserApi> createUser(@RequestBody UserApi user) {
    UserApi userDB = usersService.createUser(user);
    return new ResponseEntity<>(userDB, HttpStatus.CREATED);
  }

  @Secured({UserRoleNames.ADMIN_ROLE})
  @PostMapping("/users/bulk")
  public ResponseEntity<List<UserApi>> createUsers(@RequestBody List<UserApi> users) {
    List<UserApi> createdUsers = usersService.createUsers(users);
    return new ResponseEntity<>(createdUsers, HttpStatus.CREATED);
  }

  @Secured({UserRoleNames.ADMIN_ROLE})
  @PutMapping("/users/{userEmail}")
  public ResponseEntity<UserApi> updateUserByEmail(@PathVariable String userEmail, @RequestBody UserApi user) {
    UserApi updatedUser = usersService.updateUserByEmail(userEmail, user);
    return new ResponseEntity<>(updatedUser, HttpStatus.OK);
  }

  @Secured({UserRoleNames.ADMIN_ROLE})
  @DeleteMapping("/users/{userEmail}")
  public ResponseEntity<Void> deleteUserByEmail(@PathVariable String userEmail) {
    usersService.deleteUserByEmail(userEmail);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
