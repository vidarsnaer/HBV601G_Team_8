package is.hi.hbv501g.hbv501gteam4.Controllers;

import is.hi.hbv501g.hbv501gteam4.Persistence.Entities.User;
import is.hi.hbv501g.hbv501gteam4.Services.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping(path = "/user")
public class UserController {

    UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }


    /**
     * Handles the user sign in
     * @param user user entity
     * @return redirects to home page if successful, otherwise index page
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public ResponseEntity<String> loginPOST(Principal principal, User user, BindingResult result, Model model, HttpSession session) {

        if(result.hasErrors()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors().toString());
        }
        User exists = userService.login(user);
        if(exists != null){
            session.setAttribute("LoggedInUser", exists);
            model.addAttribute("LoggedInUser", exists);
            return ResponseEntity.ok().body("Successfully logged in.");
        }
        else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
    }


    /**
     * Handles the user sign up
     * @param user user entity
     * @param confirmPassword confirmation of password
     * @return
     */
    @RequestMapping(value = "/signup", method = RequestMethod.POST)
    public ResponseEntity<String> signupPOST(User user, BindingResult result, Model model, HttpSession session, @RequestParam("confirm-password") String confirmPassword) {

        if(result.hasErrors()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors().toString());
        }

        if (!user.getPassword().equals(confirmPassword)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect password");
        }

        User exists = userService.findByEmail(user.getEmail());
        if(exists == null){
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String hashedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(hashedPassword);
            userService.save(user);
            User loginUser = userService.login(user);
            if(loginUser != null){
                session.setAttribute("LoggedInUser", loginUser);
                model.addAttribute("LoggedInUser", loginUser);
                return ResponseEntity.ok().body("Successfully logged in.");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("User couldn't be signed up.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    /**
     * Signs the user out
     * @param session the user session
     * @return redirects back to the index page
     */
    @RequestMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok().body("Successfully logged out.");
    }

}
