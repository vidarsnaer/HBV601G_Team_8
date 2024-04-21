package is.hi.hbv501g.hbv501gteam4.Controllers;

import is.hi.hbv501g.hbv501gteam4.Persistence.Entities.User;
import is.hi.hbv501g.hbv501gteam4.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Handles user login.
     */
    @PostMapping("/login")
    public ResponseEntity<String> loginPOST(@RequestHeader String userName, @RequestHeader String userPassword, Principal principal) {
        System.out.println(principal);
        if (principal != null) {
            return ResponseEntity.ok("Already logged in.");
        }
        System.out.println(userName);
        System.out.println(userPassword);
        User user = userService.findByName(userName); //seinna find by name and password?
        User exists = userService.login(user);
        System.out.println(user);
        System.out.println(user.getId());
        System.out.println(exists);
        if (exists != null) {
            return ResponseEntity.ok("Successfully logged in.");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Login failed");
        }
    }

    /**
     * Handles user registration.
     * @param user User entity
     * @return ResponseEntity indicating registration success or failure
     */
    @PostMapping("/signup")
    public ResponseEntity<?> signupPOST(@RequestBody User user) {
        User existsEmail = userService.findByEmail(user.getEmail());
        if (existsEmail != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email already taken.");
        }
        User existsName = userService.findByName(user.getName());
        if (existsName != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already taken.");
        }

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.save(user);
        return ResponseEntity.ok(user);
    }


}
