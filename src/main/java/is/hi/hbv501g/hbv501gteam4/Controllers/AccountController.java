package is.hi.hbv501g.hbv501gteam4.Controllers;

import is.hi.hbv501g.hbv501gteam4.Persistence.Entities.User;
import is.hi.hbv501g.hbv501gteam4.Services.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.Principal;

@RestController
public class AccountController {

    // TODO: Nota frekar id en name til að authenticatea.
    private UserService userService;

    private User user;

    @Autowired
    public AccountController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Opens up the account page if a user is logged in
     * @param model
     * @param session session id
     * @return redirects to the home page if logged in, otherwise to the index page
     */
    @GetMapping("/account/loggedinuser")
    public ResponseEntity<User> userLoggedIn(Principal principal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String test = authentication.getName();
        }
        String username = principal.getName();
        User currentUser = userService.findByName(username);
        if (currentUser != null) {
            user = currentUser;
            return ResponseEntity.ok().body(user);
        } else {
            user = null;
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(user);
        }
    }

    /**
     * Changes the name of the user
     * @param newName the new name
     * @return redirects back to the account page
     */
    @PostMapping("/account/changename")
    public ResponseEntity<String> changeName(Principal principal, @RequestParam("name") String newName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String test = authentication.getName();
        }
        String username = principal.getName();
        User currentUser = userService.findByName(username);
        String result = newName.replaceAll("\\s+", " ");

        if (!result.isEmpty()) {
            user.setName(newName);
            userService.save(user);
            return ResponseEntity.ok().body("Name successfully changed.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Empty name request.");
    }

    /**
     * Changes the email of the user
     * @param oldEmail the old/current email
     * @param newEmail the new email
     * @return redirects back to the account page
     */
    @PostMapping("/account/changeemail")
    public ResponseEntity<String> changeEmail(Principal principal, @RequestParam("old-email") String oldEmail, @RequestParam("new-email") String newEmail) {

        if (oldEmail.equals(user.getEmail())) {
            String result = newEmail.replaceAll("\\s+", " ");

            if (!result.isEmpty()) {
                user.setEmail(newEmail);
                userService.save(user);
            }
        }

        return ResponseEntity.ok().body("Email successfully changed.");
    }

    /**
     * Changes the password of the user
     * @param oldPass the old/current password
     * @param newPass the new password
     * @param confirmPass confirmation of the new password
     * @return redirects back to the account page
     */
    @PostMapping("/account/changepass")
    public ResponseEntity<String> changePassword(@RequestParam("old-pass") String oldPass, @RequestParam("new-pass") String newPass, @RequestParam("confirm-pass") String confirmPass) {

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        if (passwordEncoder.matches(oldPass, user.getPassword())) {

            if (newPass.equals(confirmPass)) {
                String result = newPass.replaceAll("\\s+", " ");

                if (!result.isEmpty()) {
                    String hashedNewPassword = passwordEncoder.encode(newPass);
                    user.setPassword(hashedNewPassword);
                    userService.save(user);
                }
            }
        }

        return ResponseEntity.ok().body("Password successfully changed.");
    }

    /**
     * Deletes the user
     * @param password the password of the user
     * @return if the password is correct then the user is redirected to the home page, otherwise the account page
     */
    @PostMapping("/account/delete")
    public ResponseEntity<String> deleteAccount(Principal principal, @RequestParam("pass") String password) {

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        if (passwordEncoder.matches(password, user.getPassword())) {
            userService.delete(user);
            return ResponseEntity.ok().body("Account successfully deleted.");
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid password.");
        }
    }
}