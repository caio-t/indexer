package bitvix.indexer.util;

/**
 * DatabaseException: Custom exception for database operation failures.
 * 
 * Wraps SQL exceptions with context and allows callers to handle errors
 * gracefully instead of forcing System.exit(0).
 */
public class DatabaseException extends RuntimeException {
    
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
