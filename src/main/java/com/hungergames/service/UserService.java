package com.hungergames.service;

import com.hungergames.dto.UserRequest;
import com.hungergames.model.User;
import com.hungergames.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Transactional
    public User createUser(UserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username '" + request.getUsername() + "' is already taken");
        }
        User user = new User(request.getUsername(), request.getStartingBalance());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public User deposit(Long userId, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive");
        User user = getUser(userId);
        user.setBalance(user.getBalance() + amount);
        return userRepository.save(user);
    }

    @Transactional
    public void deductBalance(Long userId, double amount) {
        User user = getUser(userId);
        if (user.getBalance() < amount) {
            throw new IllegalStateException("Insufficient balance");
        }
        user.setBalance(user.getBalance() - amount);
        userRepository.save(user);
    }

    @Transactional
    public void addBalance(Long userId, double amount) {
        User user = getUser(userId);
        user.setBalance(user.getBalance() + amount);
        userRepository.save(user);
    }
}
