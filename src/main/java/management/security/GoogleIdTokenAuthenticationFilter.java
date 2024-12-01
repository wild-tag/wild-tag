package management.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.base.Strings;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Optional;
import management.entities.users.UserDB;
import management.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

public class GoogleIdTokenAuthenticationFilter extends GenericFilterBean {

  private Logger logger = LoggerFactory.getLogger(GoogleIdTokenAuthenticationFilter.class);


  private final String googleClientId;
  UserRepository userRepository;

  public GoogleIdTokenAuthenticationFilter(UserRepository userRepository, String googleClientId) {
    this.googleClientId = googleClientId;
    this.userRepository = userRepository;
  }

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws ServletException, IOException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
    String idTokenString = httpServletRequest.getHeader("Authorization");
    logger.debug("idTokenString: " + idTokenString);
    if (Strings.isNullOrEmpty(idTokenString)) {
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }

    idTokenString = idTokenString.replace("Bearer ", "");

    GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(),
        new GsonFactory())
        // Specify the CLIENT_ID of the app that accesses the backend:
        .setAudience(
            Collections.singletonList(googleClientId))
        .build();

    GoogleIdToken idToken = null;
    try {
      idToken = verifier.verify(idTokenString);
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    }
    logger.debug("idToken: " + idToken);
    if(idToken == null) {
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }
    Optional<UserDB> userOptional = userRepository.findByEmail(idToken.getPayload().getEmail());
    if(userOptional.isEmpty()) {
      filterChain.doFilter(servletRequest, servletResponse);
      return;
    }
    UserDB user = userOptional.get();
    // Set the user in the security context
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null,
            Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))));

    filterChain.doFilter(servletRequest, servletResponse);
  }


}
