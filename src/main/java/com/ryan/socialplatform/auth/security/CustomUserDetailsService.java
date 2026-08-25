package com.ryan.socialplatform.auth.security;

import com.ryan.socialplatform.auth.repository.UserCredentialsRepository;
import com.ryan.socialplatform.user.entity.UserCredentials;
import com.ryan.socialplatform.user.repository.UserAppRoleRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserCredentialsRepository userCredentialsRepository;
    private final UserAppRoleRepository userAppRoleRepository;

    public CustomUserDetailsService(UserCredentialsRepository userCredentialsRepository,
                                    UserAppRoleRepository userAppRoleRepository) {
        this.userCredentialsRepository = userCredentialsRepository;
        this.userAppRoleRepository = userAppRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        UserCredentials credentials = userCredentialsRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new UsernameNotFoundException("No account for: " + identifier));

        List<String> roles = userAppRoleRepository.findAllByUserId(credentials.getUserId())
                .stream()
                .map(r -> r.getRole().name())
                .toList();

        return new CustomUserDetails(credentials, credentials.getUser(), roles);
    }
}