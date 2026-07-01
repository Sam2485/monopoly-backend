package com.BusinessGame.Vyapar.service;

import com.BusinessGame.Vyapar.entity.User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {
    // Maps accessToken -> User
    private final Map<String, User> tokenMap = new ConcurrentHashMap<>();

    public String generateToken(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenMap.put(token, user);
        return token;
    }

    public User getUserByToken(String token) {
        return tokenMap.get(token);
    }

    public void removeToken(String token) {
        tokenMap.remove(token);
    }
}
