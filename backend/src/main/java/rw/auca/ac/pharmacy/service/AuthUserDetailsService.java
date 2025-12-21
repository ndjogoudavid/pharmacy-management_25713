package rw.auca.ac.pharmacy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import rw.auca.ac.pharmacy.model.core.Person;
import rw.auca.ac.pharmacy.repository.PersonRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthUserDetailsService implements UserDetailsService {

    private final PersonRepository personRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Person person = personRepository.findByEmail(email);
        if (person == null) {
            throw new UsernameNotFoundException("User not found");
        }
        String roleName = person.getRole() != null ? person.getRole().name() : "USER";
        return User.withUsername(person.getEmail())
                .password(person.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + roleName)))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
