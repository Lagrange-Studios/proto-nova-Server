package socket;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import main.Console;

public class TokenManager {
    
    private static final int TOKEN_LENGTH = 32; // 32 bytes = 256 bits
    private static final long INITIAL_TOKEN_EXPIRY = 7 * 24 * 60 * 60 * 1000; // 7 days in milliseconds
    private static final long RENEWED_TOKEN_EXPIRY = 30 * 24 * 60 * 60 * 1000; // 30 days in milliseconds
    private String serverToken;
    private Console console;
    private Map<String, ClientToken> clientTokens;
    
    private static class ClientToken {
        String token;
        long issuedAt;
        long expiresAt;
        
        ClientToken(String token, long issuedAt, long expiresAt) {
            this.token = token;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
        }
    }
    
    public TokenManager(Console console) {
        this.console = console;
        this.clientTokens = new HashMap<>();
        generateServerToken();
    }
    
    private void generateServerToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        this.serverToken = Base64.getEncoder().encodeToString(randomBytes);
        console.print("✓ Server token generated: " + serverToken.substring(0, 16) + "... (hidden for security)");
    }
    
    public String getServerToken() {
        return serverToken;
    }
    
    public String generateClientToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String clientToken = Base64.getEncoder().encodeToString(randomBytes);
        
        long now = System.currentTimeMillis();
        long expiresAt = now + INITIAL_TOKEN_EXPIRY;
        clientTokens.put(clientToken, new ClientToken(clientToken, now, expiresAt));
        
        return clientToken;
    }
    
    public boolean validateClientToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        
        ClientToken clientToken = clientTokens.get(token);
        if (clientToken == null) {
            return false;
        }
        
        long now = System.currentTimeMillis();
        if (now > clientToken.expiresAt) {
            clientTokens.remove(token);
            return false;
        }
        
        return true;
    }
    
    public String generateRenewedToken(String oldToken) {
        if (!validateClientToken(oldToken)) {
            return null;
        }
        
        clientTokens.remove(oldToken);
        
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String newToken = Base64.getEncoder().encodeToString(randomBytes);
        
        long now = System.currentTimeMillis();
        long expiresAt = now + RENEWED_TOKEN_EXPIRY;
        clientTokens.put(newToken, new ClientToken(newToken, now, expiresAt));
        
        return newToken;
    }
    
    public void revokeClientToken(String token) {
        if (token != null) {
            clientTokens.remove(token);
        }
    }
    
    public void cleanupExpiredTokens() {
        long now = System.currentTimeMillis();
        clientTokens.entrySet().removeIf(entry -> 
            now > entry.getValue().expiresAt
        );
    }
}
