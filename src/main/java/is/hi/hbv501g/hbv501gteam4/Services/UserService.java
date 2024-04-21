package is.hi.hbv501g.hbv501gteam4.Services;

import is.hi.hbv501g.hbv501gteam4.Persistence.Entities.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService extends UserDetailsService {

    User save(User user);
    void delete(User user);
    List<User> findAll();
    User findByEmail(String email);
    User findByName(String name);
    User findById(long id);
    User login(User user);
    User getCurrentUser();

    @Override
    UserDetails loadUserByUsername(String userEmail) throws UsernameNotFoundException;
}
