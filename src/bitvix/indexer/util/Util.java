package bitvix.indexer.util;

/**
 * Util: Legacy utility class for backward compatibility.
 * 
 * This class now delegates to DatabaseConfig for configuration management.
 * Database credentials are now externalizable via:
 * - database.properties file
 * - Environment variables (DB_URL, DB_USERNAME, DB_PASSWORD)
 * - System properties (db.url, db.username, db.password)
 * 
 * For direct access to configuration, use DatabaseConfig instead.
 * This class maintains backward compatibility with existing code.
 */
public class Util {
	public static final String URL = DatabaseConfig.getUrl();
	public static final String USERNAME = DatabaseConfig.getUsername();
	public static final String PASSWORD = DatabaseConfig.getPassword();
}
