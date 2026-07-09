package com.api.batterymantra.security;

import com.api.batterymantra.entity.User;
import com.api.batterymantra.entity.UserPrincipal;
import com.api.batterymantra.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(identifier);

        if (user == null) {
            user = userRepository.findByEmail(identifier);
        }

        if (user == null) {
            user = userRepository.findByPhoneNumber(identifier);
        }

        if (user == null) {
            System.out.println("User not found: " + identifier);
            throw new UsernameNotFoundException("User not found");
        }

        return new UserPrincipal(user);
    }
}
