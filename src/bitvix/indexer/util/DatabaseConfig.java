package bitvix.indexer.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * DatabaseConfig: Externalizable database configuration management.
 * 
 * Loads credentials from database.properties file in classpath.
 * Falls back to environment variables or system properties if file not found.
 * 
 * Priority order:
 * 1. database.properties (in classpath or working directory)
 * 2. Environment variables: DB_URL, DB_USERNAME, DB_PASSWORD
 * 3. System properties: db.url, db.username, db.password
 * 4. Hardcoded defaults (for backward compatibility)
 */
public class DatabaseConfig {
    private static final String PROPERTIES_FILE = "database.properties";
    
    // Default values (backward compatible with original Util.java)
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/webcrawler";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "";
    
    private static String dbUrl;
    private static String dbUsername;
    private static String dbPassword;
    
    static {
        loadConfiguration();
    }
    
    /**
     * Loads database configuration from multiple sources in priority order.
     */
    private static void loadConfiguration() {
        // Try to load from properties file first
        Properties props = loadPropertiesFile();
        
        if (props != null) {
            dbUrl = props.getProperty("db.url", DEFAULT_URL);
            dbUsername = props.getProperty("db.username", DEFAULT_USERNAME);
            dbPassword = props.getProperty("db.password", DEFAULT_PASSWORD);
        } else {
            // Fall back to environment variables
            dbUrl = getFromEnvironmentOrDefault("DB_URL", "db.url", DEFAULT_URL);
            dbUsername = getFromEnvironmentOrDefault("DB_USERNAME", "db.username", DEFAULT_USERNAME);
            dbPassword = getFromEnvironmentOrDefault("DB_PASSWORD", "db.password", DEFAULT_PASSWORD);
        }
        
        logConfiguration();
    }
    
    /**
     * Attempts to load database.properties from classpath.
     */
    private static Properties loadPropertiesFile() {
        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (input != null) {
                Properties props = new Properties();
                props.load(input);
                System.out.println("✓ Database configuration loaded from: " + PROPERTIES_FILE);
                return props;
            }
        } catch (IOException e) {
            System.err.println("⚠ Failed to load " + PROPERTIES_FILE + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Gets value from environment variable, system property, or default.
     */
    private static String getFromEnvironmentOrDefault(String envVar, String sysProp, String defaultValue) {
        String value = System.getenv(envVar);
        if (value != null && !value.isEmpty()) {
            System.out.println("✓ Using environment variable: " + envVar);
            return value;
        }
        
        value = System.getProperty(sysProp);
        if (value != null && !value.isEmpty()) {
            System.out.println("✓ Using system property: " + sysProp);
            return value;
        }
        
        System.out.println("✓ Using default for: " + envVar);
        return defaultValue;
    }
    
    /**
     * Logs the loaded configuration (without exposing sensitive data).
     */
    private static void logConfiguration() {
        System.out.println("=== Database Configuration ===");
        System.out.println("URL: " + dbUrl);
        System.out.println("Username: " + dbUsername);
        System.out.println("Password: " + (dbPassword.isEmpty() ? "(empty)" : "****"));
        System.out.println("==============================");
    }
    
    // Public accessors
    public static String getUrl() {
        return dbUrl;
    }
    
    public static String getUsername() {
        return dbUsername;
    }
    
    public static String getPassword() {
        return dbPassword;
    }
    
    /**
     * Reloads configuration (useful for testing or dynamic updates).
     */
    public static void reload() {
        loadConfiguration();
    }
}
