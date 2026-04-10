package com.hungergames.controller;

import com.hungergames.model.User;
import com.hungergames.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/")
        public String home() {
            return "redirect:/login";
        }

    @GetMapping("/login")
    public String showLogin() {
        return "login"; 
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username, 
                               @RequestParam String password, 
                               Model model) {
        

        if (userRepository.existsByUsername(username)) {
            model.addAttribute("error", "Ez a felhasználónév már foglalt!");
            return "login";
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));

        System.out.println("New user: " + newUser);
        
        userRepository.save(newUser);
        return "redirect:/login"; 
    }
}