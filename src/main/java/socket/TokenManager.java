package socket;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import main.Console;

/**
 * Manages server tokens for client authentication.
 * A new token is generated each time the server starts.
 * Tokens are used to authenticate clients before they can join the game.
 */
public class TokenManager {
    
    private static final int TOKEN_LENGTH = 32; // 32 bytes = 256 bits
    private String serverToken;
    private Console console;
    private Map<String, ClientToken> clientTokens; // Maps client tokens to their issue time
    
    private static class ClientToken {
        String token;
        long issuedAt;
        
        ClientToken(String token, long issuedAt) {
            this.token = token;
            this.issuedAt = issuedAt;
        }
    }
    
    public TokenManager(Console console) {
        this.console = console;
        this.clientTokens = new HashMap<>();
        generateServerToken();
    }
    
    /**
     * Generate a new cryptographically secure random token for the server
     */
    private void generateServerToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        this.serverToken = Base64.getEncoder().encodeToString(randomBytes);
        console.print("✓ Server token generated: " + serverToken.substring(0, 16) + "... (hidden for security)");
    }
    
    /**
     * Get the current server token
     */
    public String getServerToken() {
        return serverToken;
    }
    
    /**
     * Generate a new client token and return it
     */
    public String generateClientToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String clientToken = Base64.getEncoder().encodeToString(randomBytes);
        
        // Store the token
        clientTokens.put(clientToken, new ClientToken(clientToken, System.currentTimeMillis()));
        
        return clientToken;
    }
    
    /**
     * Validate a client token
     */
    public boolean validateClientToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        return clientTokens.containsKey(token);
    }
    
    /**
     * Revoke a client token (after they've authenticated)
     */
    public void revokeClientToken(String token) {
        if (token != null) {
            clientTokens.remove(token);
        }
    }
    
    /**
     * Clean up expired client tokens (optional, can be called periodically)
     * Tokens are valid for 5 minutes by default
     */
    public void cleanupExpiredTokens(long expiryMs) {
        long now = System.currentTimeMillis();
        clientTokens.entrySet().removeIf(entry -> 
            now - entry.getValue().issuedAt > expiryMs
        );
    }
}
