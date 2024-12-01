package management.configuration;

import management.repositories.UserRepository;
import management.security.GoogleIdTokenAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

  @Autowired
  UserRepository userRepository;

  @Value("${google.api.key}")
  private String googleClientId;

  @Bean
  @Order(2)
  @Autowired
  public SecurityFilterChain authenticationFilterChain(HttpSecurity http) throws Exception {
    RequestMatcher requestMatcher = new AntPathRequestMatcher("/**");
    http.securityMatcher(requestMatcher);
    http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
        .addFilterAfter(new GoogleIdTokenAuthenticationFilter(userRepository, googleClientId),
            AbstractPreAuthenticatedProcessingFilter.class)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .csrf(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable);

    return http.build();
  }

//  @Bean
//  public WebMvcConfigurer corsConfigurer() {
//    return new WebMvcConfigurer() {
//      @Override
//      public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//            .allowedOrigins("http://localhost:3000")
//            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//            .allowedHeaders("*")
//            .allowCredentials(true);
//      }
////    };
//  }

}
