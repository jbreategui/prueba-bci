package com.brr.customers.api.service;

import com.brr.customers.api.domain.Phone;
import com.brr.customers.api.domain.User;
import com.brr.customers.api.dto.PhoneRequest;
import com.brr.customers.api.dto.PhoneResponse;
import com.brr.customers.api.dto.SignUpRequest;
import com.brr.customers.api.dto.SignUpResponse;
import com.brr.customers.api.exception.EmailAlreadyRegisteredException;
import com.brr.customers.api.exception.UserNotFoundException;
import com.brr.customers.api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final JwtService jwtService;


    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public SignUpResponse findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(UserNotFoundException::new);
        return toSignUpResponse(user);
    }

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setLastLogin(LocalDateTime.now());
        user.setActive(true);
        user.setToken("");

        request.phones().stream()
                .map(this::toPhone)
                .forEach(user::addPhone);

        User saved = userRepository.saveAndFlush(user);
        saved.setToken(jwtService.generate(saved));
        saved = userRepository.saveAndFlush(saved);


        return toSignUpResponse(saved);
    }


    private Phone toPhone(PhoneRequest dto) {
        Phone phone = new Phone();
        phone.setNumber(dto.number());
        phone.setCityCode(dto.citycode());
        phone.setCountryCode(dto.contrycode());
        return phone;
    }


    private PhoneResponse toPhoneResponse(Phone phone) {
        return new PhoneResponse(
                phone.getNumber(),
                phone.getCityCode(),
                phone.getCountryCode()
        );
    }

    private SignUpResponse toSignUpResponse(User user) {
        List<PhoneResponse> phones = user.getPhones().stream()
                .map(this::toPhoneResponse)
                .toList();

        return new SignUpResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                phones,
                user.getCreated(),
                user.getModified(),
                user.getLastLogin(),
                user.getToken(),
                user.isActive()
        );
    }


}
