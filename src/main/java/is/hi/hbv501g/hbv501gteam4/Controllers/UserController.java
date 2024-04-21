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
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Handles user login.
     * @param user User entity
     * @param principal Principal to ensure the request is authenticated
     * @return ResponseEntity indicating login success or failure
     */
    @PostMapping("/login")
    public ResponseEntity<String> loginPOST(@RequestHeader String userEmail, @RequestHeader String userPassword, Principal principal) {
        System.out.println(principal);
        if (principal != null) {
            return ResponseEntity.ok("Already logged in.");
        }
        System.out.println(userEmail);
        System.out.println(userPassword);
        User user = userService.findByEmail(userEmail); //seinna find by email and password
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
        User exists = userService.findByEmail(user.getEmail());
        if (exists != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists.");
        }

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userService.save(user);
        return ResponseEntity.ok(user);
    }


}
