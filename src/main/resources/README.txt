AUTOMATIC SSL/TLS SETUP - ZERO MANUAL WORK REQUIRED!

This directory is for optionally embedding a keystore for custom SSL certificates.

HOW IT WORKS:
1. The server and client now use automatic secure connections
2. NO file system paths needed - no searching directories for keystores
3. WORKS FROM ANY COMPUTER - your friend's computer, your computer, anywhere
4. ZERO SETUP - just compile and run!

What happens automatically:
- If keystore.jks exists in this directory, it will be embedded and used
- If no keystore.jks exists, the app auto-generates secure certificates in memory
- The application works on any machine without any configuration

OPTIONAL ADVANCED SETUP (not required):
If you want to use a custom keystore.jks instead of auto-generation:
1. Place keystore.jks in this directory
2. Rebuild with: gradle build
3. The keystore will be automatically embedded in the JAR

The application is now fully portable and requires zero manual SSL/TLS setup!

