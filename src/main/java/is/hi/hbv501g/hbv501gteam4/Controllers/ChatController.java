package is.hi.hbv501g.hbv501gteam4.Controllers;

import is.hi.hbv501g.hbv501gteam4.Persistence.Entities.Conversation;
import is.hi.hbv501g.hbv501gteam4.Persistence.Entities.Message;
import is.hi.hbv501g.hbv501gteam4.Persistence.Entities.User;
import is.hi.hbv501g.hbv501gteam4.Services.ConversationService;
import is.hi.hbv501g.hbv501gteam4.Services.MessageService;
import is.hi.hbv501g.hbv501gteam4.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ConversationService conversationService;
    private final UserService userService;
    private final MessageService messageService;

    @Autowired
    public ChatController(ConversationService conversationService, UserService userService, MessageService messageService) {
        this.conversationService = conversationService;
        this.userService = userService;
        this.messageService = messageService;
    }

    // Get all conversations for the logged-in user
    @GetMapping
    public ResponseEntity<List<Conversation>> getAllConversations(Principal principal) {
        User currentUser = userService.findByName(principal.getName());
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Conversation> conversations = conversationService.findBySellerIdOrBuyerId(currentUser.getId());
        return ResponseEntity.ok(conversations);
    }

    // Get a specific conversation
    @GetMapping("/{id}")
    public ResponseEntity<Conversation> getConversation(Principal principal, @PathVariable("id") long conversationId) {
        User currentUser = userService.findByName(principal.getName());
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Conversation conversation = conversationService.findByConversationID(conversationId);
        if (conversation == null || (conversation.getBuyerID() != currentUser.getId() && conversation.getSellerID() != currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(conversation);
    }

    // End a conversation
    @PostMapping("/end/{id}")
    public ResponseEntity<String> endConversation(Principal principal, @PathVariable("id") long conversationId) {
        User currentUser = userService.findByName(principal.getName());
        Conversation conversation = conversationService.findByConversationID(conversationId);
        if (conversation == null || (conversation.getBuyerID() != currentUser.getId() && conversation.getSellerID() != currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        conversation.setConversationEnded(true);
        conversationService.save(conversation);
        return ResponseEntity.ok("Conversation successfully ended.");
    }

    // Send a message in a conversation
    @PostMapping("/send/{id}")
    public ResponseEntity<String> sendMessage(Principal principal, @PathVariable("id") long conversationId, @RequestParam("message") String messageText) {
        User currentUser = userService.findByName(principal.getName());
        Conversation conversation = conversationService.findByConversationID(conversationId);
        if (conversation == null || (conversation.getBuyerID() != currentUser.getId() && conversation.getSellerID() != currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Message message = new Message(conversationId, currentUser.getId(), messageText.trim());
        messageService.save(message);
        return ResponseEntity.ok("Message sent successfully.");
    }

    // Create a conversation
    @PostMapping("/create/{sellerId}/{title}")
    public ResponseEntity<Conversation> createConversation(Principal principal, @PathVariable("sellerId") long sellerId, @PathVariable("title") String title) {
        User currentUser = userService.findByName(principal.getName());
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Conversation conversation = new Conversation(currentUser.getId(), sellerId, title.trim());
        conversationService.save(conversation);
        return ResponseEntity.ok(conversation);
    }

    // Specialized method to start a conversation with customer service
    @PostMapping("/customer-service")
    public ResponseEntity<Conversation> startConversationWithCustomerService(Principal principal) {
        User currentUser = userService.findByName(principal.getName());
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Conversation conversation = new Conversation(currentUser.getId(), 10, "Customer Service");
        conversationService.save(conversation);
        return ResponseEntity.ok(conversation);
    }
}
