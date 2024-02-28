package is.hi.hbv501g.hbv501gteam4.Config;

import is.hi.hbv501g.hbv501gteam4.Services.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {


    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // If CSRF is deprecated and your application is secure without it, ensure this configuration aligns with the latest recommendations.
                // .csrf().disable()
                .authorizeHttpRequests(authz -> authz
                        // Update this part according to the new method provided by Spring Security if antMatchers() is deprecated.
                        .requestMatchers("/login", "/signup").permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
