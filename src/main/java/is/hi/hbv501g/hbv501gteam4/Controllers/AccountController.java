package is.hi.hbv501g.hbv501gteam4.Controllers;

import is.hi.hbv501g.hbv501gteam4.Persistence.Entities.User;
import is.hi.hbv501g.hbv501gteam4.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserService userService;

    @Autowired
    public AccountController(UserService userService) {
        this.userService = userService;
    }

    // Get the currently logged in user's details
    @GetMapping("/loggedinuser")
    public ResponseEntity<?> getLoggedInUser(Principal principal) {
        User user = userService.findByName(principal.getName());
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // Change the logged in user's name
    @PostMapping("/changeName")
    public ResponseEntity<String> changeName(Principal principal, @RequestParam("name") String newName) {
        User user = userService.findByName(principal.getName());
        if (newName == null || newName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Empty name request.");
        }

        user.setName(newName.trim());
        userService.save(user);
        return ResponseEntity.ok("Name successfully changed.");
    }

    // Change the logged in user's email
    @PostMapping("/changeEmail")
    public ResponseEntity<String> changeEmail(Principal principal, @RequestParam("old-email") String oldEmail, @RequestParam("new-email") String newEmail) {
        User user = userService.findByName(principal.getName());
        if (!oldEmail.equals(user.getEmail())) {
            return ResponseEntity.badRequest().body("Old email does not match current email.");
        }

        user.setEmail(newEmail.trim());
        userService.save(user);
        return ResponseEntity.ok("Email successfully changed.");
    }

    // Change the logged in user's password
    @PostMapping("/changePass")
    public ResponseEntity<String> changePassword(Principal principal, @RequestParam("old-pass") String oldPass, @RequestParam("new-pass") String newPass, @RequestParam("confirm-pass") String confirmPass) {
        if (!newPass.equals(confirmPass)) {
            return ResponseEntity.badRequest().body("Passwords do not match.");
        }

        User user = userService.findByName(principal.getName());
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (!passwordEncoder.matches(oldPass, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect old password.");
        }

        user.setPassword(passwordEncoder.encode(newPass.trim()));
        userService.save(user);
        return ResponseEntity.ok("Password successfully changed.");
    }

    // Delete the logged in user's account
    @PostMapping("/delete")
    public ResponseEntity<String> deleteAccount(Principal principal, @RequestParam("pass") String password) {
        User user = userService.findByName(principal.getName());
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password.");
        }

        userService.delete(user);
        return ResponseEntity.ok("Account successfully deleted.");
    }
}
