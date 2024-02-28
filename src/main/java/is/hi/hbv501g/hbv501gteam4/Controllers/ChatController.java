package is.hi.hbv501g.hbv501gteam4.Controllers;

import is.hi.hbv501g.hbv501gteam4.Persistence.Entities.Conversation;
import is.hi.hbv501g.hbv501gteam4.Persistence.Entities.Message;
import is.hi.hbv501g.hbv501gteam4.Persistence.Entities.User;
import is.hi.hbv501g.hbv501gteam4.Services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@RestController
public class ChatController {

    ConversationService conversationService;
    UserService userService;
    MessageService messageService;




    @Autowired
    public ChatController(ConversationService conversationService, UserService userService, MessageService messageService) {
        this.conversationService = conversationService;
        this.userService = userService;
        this.messageService = messageService;
    }

    /**
     * Opens up the account page if a user is logged in
     * @return redirects to the chat page if logged in, otherwise to the index page
     */
    @RequestMapping("/chat")
    public ResponseEntity<List<Conversation>> chatPage(Principal principal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String test = authentication.getName();
        }
        String username = principal.getName();
        User currentUser = userService.findByName(username);
        if (currentUser != null) {
            List<Conversation> allConversations = conversationService.findBySellerIdOrBuyerId(currentUser.getId());
            List<User> contacts = new ArrayList<>();
            for (Conversation conversation : allConversations) {
                if (conversation.getBuyerID() == currentUser.getId()) {
                    contacts.add(userService.findById(conversation.getSellerID()));
                } else {
                    contacts.add(userService.findById(conversation.getBuyerID()));
                }
            }
            //TODO: method fyrir contacts?
            return ResponseEntity.ok().body(allConversations);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    /**
     * Opens up a conversation between two users
     * @param conversationId the id of the conversation
     * @return displays the conversation
     */
    @GetMapping("/chat/{id}")
    public ResponseEntity<Conversation> selectConversation(Principal principal, @PathVariable("id") long conversationId, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String test = authentication.getName();
        }
        String username = principal.getName();
        User currentUser = userService.findByName(username);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Conversation selectedConversation = conversationService.findByConversationID(conversationId);
        List<Conversation> allConversations = conversationService.findBySellerIdOrBuyerId(currentUser.getId());

        List<Message> allMessages = messageService.findByConversationID(conversationId);

        boolean deletedUser = false;

        List<String> names = new ArrayList<>();
        for (Message message : allMessages) {
            if(message.getSenderID() == currentUser.getId()) {
                names.add("You");
            } else {
                User user = userService.findById(message.getSenderID());
                if (user != null) {
                    names.add(user.getName());
                } else {
                    names.add("Deleted user");
                    deletedUser = true;
                }
            }
        }

        if (deletedUser) {
            selectedConversation.setConversationEnded(true);
            conversationService.save(selectedConversation);
        }

        model.addAttribute("currentConversation", selectedConversation);
        model.addAttribute("conversations", allConversations);

        model.addAttribute("activeConversation", !selectedConversation.isConversationEnded());

        model.addAttribute("messages", allMessages);
        model.addAttribute("names", names);

        return ResponseEntity.ok().body(selectedConversation);
    }

    /**
     * Ends a conversation between two users
     * @param conversationId the id of the conversation
     * @return redirects back to the chat page
     */
    @GetMapping("/endChat/{conversationId}")
    public ResponseEntity<String> endChat(Principal principal, @PathVariable("conversationId") long conversationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String test = authentication.getName();
        }
        String username = principal.getName();
        User currentUser = userService.findByName(username);
        Conversation conversation = conversationService.findByConversationID(conversationId);
        if (conversation != null) {
            conversation.setConversationEnded(true);
            conversationService.save(conversation);
            return ResponseEntity.ok().body("Conversation successfully ended.");
        }
        else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }


    /**
     * Sends a message in a conversation
     * @param conversationId the id of the conversation
     * @param messageText the message
     * @return redirects to the same page (refresh)
     */
    @PostMapping("/send-message/{conversationId}")
    public ResponseEntity<String> sendMessage(Principal principal, @PathVariable("conversationId") long conversationId, @RequestParam("message") String messageText) {
        Conversation conversation = conversationService.findByConversationID(conversationId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String test = authentication.getName();
        }
        String username = principal.getName();
        User currentUser = userService.findByName(username);

        if (conversation != null) {
            if (currentUser != null) {
                Message message = new Message(conversationId, currentUser.getId(), messageText);

                messageService.save(message);

                return ResponseEntity.ok().body("Message delivered.");
            }
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authorized.");
        }
        return ResponseEntity.badRequest().body("Invalid request.");
    }


    /**
     * Creates a conversation between two users
     * @param sellerId the id of the seller
     * @param title the title of the conversation
     * @return redirects to the new conversation
     */
    @PostMapping("/create-conversation/{sellerId}/{title}")
    public ResponseEntity<Conversation> createConversation(Principal principal, @PathVariable("sellerId") long sellerId, @PathVariable("title") String title) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String test = authentication.getName();
        }
        String username = principal.getName();
        User currentUser = userService.findByName(username);
        if (currentUser != null) {
            Conversation conversation = new Conversation(currentUser.getId(), sellerId, title);

            conversationService.save(conversation);

            return ResponseEntity.ok().body(conversation);

        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    /**
     * Starts a conversation with customer service
     * @return redirects to chats and opens the conversation with Customer Service
     */
    @GetMapping("/create-conversation/customer-service")
    public ResponseEntity<Conversation> createConversationCustomerService(Principal principal) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            String test = authentication.getName();
        }
        String username = principal.getName();
        User currentUser = userService.findByName(username);
        if (currentUser != null) {
            Conversation conversation = new Conversation(currentUser.getId(), 10, "Customer Service");

            conversationService.save(conversation);

            return ResponseEntity.ok().body(conversation);

        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }
}
