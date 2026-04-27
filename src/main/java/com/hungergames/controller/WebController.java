package com.hungergames.controller;

import com.hungergames.model.User;
import com.hungergames.dto.UserRequest;
import com.hungergames.model.GameStatus;
import com.hungergames.repository.UserRepository;
import com.hungergames.service.GameService;
import com.hungergames.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.NoSuchElementException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GameService gameService;
    private final UserService userService;

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

        UserRequest request = new UserRequest();
        request.setUsername(username);
        request.setPassword(password);
        userService.createUser(request);
        return "redirect:/login"; 
    }

    @PostMapping("/api/login")
        public String loginUser(@RequestParam String username, 
                                @RequestParam String password, 
                                HttpServletResponse response) {

            User user = userRepository.findByUsername(username).orElse(null);


            if (user != null && passwordEncoder.matches(password, user.getPassword())) {
                

                Cookie cookie = new Cookie("userId", String.valueOf(user.getId()));
                cookie.setPath("/");
                cookie.setMaxAge(7 * 24 * 60 * 60);


                response.addCookie(cookie);
                
                return "redirect:/sim";
            }

            return "redirect:/login?error";
        }
}