package is.hi.hbv501g.hbv501gteam4.Controllers;

import is.hi.hbv501g.hbv501gteam4.Persistence.Entities.Disc;
import is.hi.hbv501g.hbv501gteam4.Persistence.Entities.Favorite;
import is.hi.hbv501g.hbv501gteam4.Persistence.Entities.Image;
import is.hi.hbv501g.hbv501gteam4.Persistence.Entities.User;
import is.hi.hbv501g.hbv501gteam4.Services.DiscService;
import is.hi.hbv501g.hbv501gteam4.Services.FavoriteService;
import is.hi.hbv501g.hbv501gteam4.Services.ImageService;
import is.hi.hbv501g.hbv501gteam4.Services.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.*;

@RestController
@RequestMapping("/disc")
public class DiscController {

    DiscService discService;
    ImageService imageService;
    UserService userService;
    FavoriteService favoriteService;

    private User user;

    @Autowired
    public DiscController(DiscService discService, ImageService imageService, UserService userService, FavoriteService favoriteService) {
        this.discService = discService;
        this.imageService = imageService;
        this.userService = userService;
        this.favoriteService = favoriteService;
    }


    @GetMapping("/all")
    public ResponseEntity<List<Disc>> indexPageLoggedIn(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.findByName(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Disc> allDiscs = discService.findAll();
        return ResponseEntity.ok(allDiscs);
    }

    /**
     * Displays the favorite discs of the logged-in user.
     * @param principal Principal object to fetch the authenticated user's details.
     * @return ResponseEntity with list of favorite discs or UNAUTHORIZED if no user is found.
     */
    @GetMapping("/home/favorites")
    public ResponseEntity<List<Disc>> homePageFavorites(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.findByName(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Favorite> favorites = favoriteService.findFavoritesByUser(user);
        List<Disc> favoriteDiscs = new ArrayList<>();
        for (Favorite favorite : favorites) {
            favoriteDiscs.add(favorite.getDisc());
        }

        return ResponseEntity.ok(favoriteDiscs);
    }


    /**
     * Adds a disc to the database, handling image uploads.
     * @param disc the disc being added
     * @param result to capture validation results
     * @param images images that were uploaded with the new disc
     * @param principal to identify the logged-in user
     * @return ResponseEntity with status message
     */
    @PostMapping("/addDisc")
    public ResponseEntity<String> addDiscPOST(@RequestBody Disc disc, BindingResult result,
                                              @RequestParam("image") MultipartFile[] images,
                                              Principal principal) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body("Error in disc data.");
        }

        User user = userService.findByName(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User must be logged in.");
        }

        disc.setUser(user);
        Disc discSaved = discService.save(disc);
        if (addImage(discSaved, images)) {
            return ResponseEntity.status(HttpStatus.CREATED).body("Disc successfully added with images.");
        } else {
            return ResponseEntity.ok("Disc added, but failed to add images.");
        }
        //TODO: skila nýja Disc?
    }


    /**
     * Updates the disc information in the database and handles image updates.
     * @param id The ID of the disc to update.
     * @param disc The updated disc information.
     * @param result BindingResult to handle validation results.
     * @param images Array of MultipartFile images that are being updated with the disc.
     * @param principal Principal object to ensure the user is authorized.
     * @return ResponseEntity with status message.
     */
    @PostMapping("/update/{id}")
    public ResponseEntity<String> updateDiscPOST(@PathVariable long id, @RequestBody Disc disc, BindingResult result,
                                                 @RequestParam("image") MultipartFile[] images, Principal principal) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body("Validation errors: " + result.getAllErrors());
        }

        User user = userService.findByName(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User must be logged in to update discs.");
        }

        Disc existingDisc = discService.findBydiscID(id);
        if (existingDisc == null) {
            return ResponseEntity.notFound().build();
        }

        // Check if the logged-in user is authorized to update this disc
        if (!existingDisc.getUserId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized to update this disc.");
        }

        Disc discSaved = discService.save(disc);
        boolean imagesUpdated = addImage(discSaved, images);
        if (imagesUpdated) {
            return ResponseEntity.ok("Disc and images successfully updated.");
        } else {
            return ResponseEntity.ok("Disc updated, but some images failed to update.");
        }
    }

    /**
     * Deletes a disc from the database.
     * @param id The ID of the disc that should be deleted.
     *           @param principal Principal object to ensure the user is authorized.
     * @return ResponseEntity with status message.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDisc(@PathVariable("id") long id, Principal principal) {
        User user = userService.findByName(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User must be logged in to update discs.");
        }
        Disc discToDelete = discService.findBydiscID(id);
        if (discToDelete != null) {
            discService.delete(discToDelete);
            return ResponseEntity.ok("Successfully deleted disc.");
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Deletes an image associated with a disc.
     * @param id The ID of the disc the image is connected to.
     * @param imageId The ID of the image to delete.
     * @param principal Principal object to ensure the user is authorized.
     * @return ResponseEntity with status message.
     */
    @DeleteMapping("/{id}/images/{imageId}")
    public ResponseEntity<String> deleteImage(@PathVariable("id") long id,
                                              @PathVariable("imageId") long imageId,
                                              Principal principal) {
        User user = userService.findByName(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User must be logged in to update discs.");
        }

        // Optional: Check if the user is authorized to delete the image
        Image imageToDelete = imageService.findByimageID(imageId);
        // TODO: Bæta við checki hvort imageId sé ekki örugglega tengt við diskinn með þetta id
        // !imageToDelete.getDisc().getId().equals(id)
        if (imageToDelete == null) {
            return ResponseEntity.notFound().build();
        }

        // Optional: Check if the logged-in user has permissions to delete this particular image
        Disc disc = discService.findBydiscID(id);
        if (!disc.getUserId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized to delete this image.");
        }

        try {
            imageService.delete(imageToDelete);
            return ResponseEntity.ok("Successfully deleted image.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to delete image due to internal error.");
        }
    }

    /**
     * Function to add an image to the bucket in supabase
     * @param disc the disc that the image is connected to
     * @param images the image that needs to be uploaded
     */
    private boolean addImage(Disc disc, MultipartFile[] images) {
        boolean allImagesSaved = true;
        for (MultipartFile image : images) {
            if (!image.isEmpty()) {
                try {
                    byte[] fileBytes = image.getBytes();
                    String mimeType = image.getContentType();

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFjcnFqenlvY3R5dmh1a3pib2RuIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTY5NTA0Njk1MCwiZXhwIjoyMDEwNjIyOTUwfQ.p1wvVmxqJTOhDEBCUxrDurXrg7m5MTDBiXHVNT55Ws8");

                    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                    body.add("file", new HttpEntity<>(fileBytes, headers));
                    body.add("metadata", "{\"discID\":\"" + disc.getDiscID() + "\", \"mimeType\":\"" + mimeType + "\"}");

                    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                    RestTemplate restTemplate = new RestTemplate();
                    String supabaseUrl = "https://qcrqjzyoctyvhukzbodn.supabase.co/storage/v1/object/public/Images/Discs/";
                    restTemplate.postForEntity(supabaseUrl, requestEntity, String.class);

                    // Assuming Image has a method to set the URL directly
                    Image imageEntity = new Image();
                    //imageEntity.setDisc(disc);
                    imageEntity.setImage(supabaseUrl + disc.getDiscID()); // Simplified URL handling
                    imageService.save(imageEntity);

                } catch (Exception e) {
                    e.printStackTrace();
                    allImagesSaved = false;
                }
            }
        }
        return allImagesSaved;
    }


    /**
     * Retrieves and displays the details of the disc with the specified ID,
     * along with its favorite status and associated images.
     *
     * @param id the identifier of the disc to retrieve details for.
     * @param principal Principal object to fetch the authenticated user's details.
     * @return ResponseEntity containing the disc details if found, otherwise an error message.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> discDetails(@PathVariable("id") long id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = principal.getName();
        User user = userService.findByName(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Disc disc = discService.findBydiscID(id);
        if (disc == null) {
            return ResponseEntity.notFound().build();
        }

        // Retrieve favorite status
        Favorite favorite = favoriteService.findFavoriteByUserAndDisc(user, disc);
        boolean isFavorite = favorite != null;
        Long favoriteId = isFavorite ? favorite.getId() : null;

        // Retrieve a list of the images related to that specific disc
        List<Image> images = disc.getImages();

        // Construct a more detailed response if needed
        DiscDetails details = new DiscDetails(disc, images, isFavorite, favoriteId);

        return ResponseEntity.ok(details);
    }

    static class DiscDetails {
        private Disc disc;
        private List<Image> images;
        private boolean isFavorite;
        private Long favoriteId;

        public DiscDetails(Disc disc, List<Image> images, boolean isFavorite, Long favoriteId) {
            this.disc = disc;
            this.images = images;
            this.isFavorite = isFavorite;
            this.favoriteId = favoriteId;
        }

        // Getters and setters for serialization
        public Disc getDisc() {
            return disc;
        }

        public void setDisc(Disc disc) {
            this.disc = disc;
        }

        public List<Image> getImages() {
            return images;
        }

        public void setImages(List<Image> images) {
            this.images = images;
        }

        public boolean isFavorite() {
            return isFavorite;
        }

        public void setFavorite(boolean favorite) {
            isFavorite = favorite;
        }

        public Long getFavoriteId() {
            return favoriteId;
        }

        public void setFavoriteId(Long favoriteId) {
            this.favoriteId = favoriteId;
        }
    }

    /**
     * Adds a disc to the favorites for the authenticated user.
     * @param discId the ID of the disc that should be added to favorites
     * @param principal Principal object to fetch the authenticated user's details
     * @return ResponseEntity with status message
     */
    @PostMapping("/favorites/{discId}")
    public ResponseEntity<String> addToFavorites(@PathVariable("discId") long discId, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User must be logged in to add favorites.");
        }

        String username = principal.getName();
        User user = userService.findByName(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        Disc disc = discService.findBydiscID(discId);
        if (disc == null) {
            return ResponseEntity.notFound().build();
        }

        Favorite favorite = new Favorite(user, disc);
        favoriteService.save(favorite);
        return ResponseEntity.ok("Successfully added disc to favorites.");
    }

    /**
     * Removes a disc from favorites based on the favorite ID.
     * @param discId ID of the disc that should be removed from favorites
     * @param favoriteId ID of the favorite record to be deleted
     * @param principal Principal object to fetch the authenticated user's details
     * @return ResponseEntity with status message
     */
    @DeleteMapping("/{discId}/{favoriteId}")
    public ResponseEntity<String> removeFromFavorites(@PathVariable("discId") long discId,
                                                      @PathVariable("favoriteId") long favoriteId,
                                                      Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User must be logged in.");
        }

        User user = userService.findByName(principal.getName());
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found.");
        }

        Favorite favorite = favoriteService.findById(favoriteId);
        if (favorite == null || !favorite.getUser().equals(user)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Favorite not found or user does not own this favorite.");
        }

        if (favorite.getDisc().getDiscID() != discId) {
            return ResponseEntity.badRequest().body("Mismatched disc ID in favorite.");
        }

        favoriteService.delete(favorite);
        return ResponseEntity.ok("Successfully removed disc from favorites.");
    }

    /**
     * Filters and searches for discs based on various criteria.
     *
     * @param fromPrice the minimum price
     * @param toPrice the maximum price
     * @param colour the colour of the disc
     * @param condition the condition of the disc (used or new)
     * @param name the name of the disc
     * @param principal Principal object to fetch the authenticated user's details
     * @return ResponseEntity with filtered list of discs
     */
    @GetMapping("/filter")
    public ResponseEntity<List<Disc>> filterDiscs(
            @RequestParam(value = "fromPrice", required = false) Integer fromPrice,
            @RequestParam(value = "toPrice", required = false) Integer toPrice,
            @RequestParam(value = "colour", required = false) String colour,
            @RequestParam(value = "condition", required = false) String condition,
            @RequestParam(value = "name", required = false) String name,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Disc> filteredDiscs = discService.findAll();

        if (name != null && !name.isEmpty()) {
            filteredDiscs = filteredDiscs.stream()
                    .filter(d -> d.getName().equalsIgnoreCase(name))
                    .collect(Collectors.toList());
        }

        if (fromPrice != null && toPrice != null) {
            filteredDiscs = filteredDiscs.stream()
                    .filter(d -> d.getPrice() >= fromPrice && d.getPrice() <= toPrice)
                    .collect(Collectors.toList());
        } else {
            if (fromPrice != null) {
                filteredDiscs = filteredDiscs.stream()
                        .filter(d -> d.getPrice() >= fromPrice)
                        .collect(Collectors.toList());
            }
            if (toPrice != null) {
                filteredDiscs = filteredDiscs.stream()
                        .filter(d -> d.getPrice() <= toPrice)
                        .collect(Collectors.toList());
            }
        }

        if (colour != null && !colour.isEmpty()) {
            filteredDiscs = filteredDiscs.stream()
                    .filter(d -> d.getColour().equalsIgnoreCase(colour))
                    .collect(Collectors.toList());
        }

        if (condition != null && !condition.isEmpty()) {
            filteredDiscs = filteredDiscs.stream()
                    .filter(d -> d.getCondition().equalsIgnoreCase(condition))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(filteredDiscs);
    }
}