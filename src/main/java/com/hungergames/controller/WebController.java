package com.hungergames.controller;

import com.hungergames.model.User;
import com.hungergames.model.GameStatus;
import com.hungergames.repository.UserRepository;
import com.hungergames.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.NoSuchElementException;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GameService gameService;

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLogin() {
        return "login"; 
    }

    @GetMapping("/sim")
    public String showSim() {
        try {
            if (gameService.getCurrentGame().getStatus() == GameStatus.BETTING) {
                return "redirect:/fogadas";
            }
        } catch (NoSuchElementException e) {
            return "redirect:/fogadas"; 
        }
        return "sim"; 
    }

    @GetMapping("/fogadas")
    public String showFogadas() {
        try {
            if (gameService.getCurrentGame().getStatus() == GameStatus.IN_PROGRESS) {
                return "redirect:/sim";
            }
        } catch (NoSuchElementException e) {

        }
        return "fogadas"; 
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
        
        userRepository.save(newUser);
        return "redirect:/login"; 
    }
}