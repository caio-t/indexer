# Copilot Instructions for Indexer Crawler

## Project Overview
This is a **Java web crawler** that periodically fetches and indexes web content matching customer keywords. It uses `crawler4j` for crawling machinery and plain JDBC for MySQL persistence.

**Key architecture**: Timer-based scheduler → Controller (orchestrates per-customer crawls) → Crawler instances (page parsing + keyword matching) → ContentDao (direct JDBC writes).

## Build & Run

### Prerequisites
- Java compiler (`javac`)
- MySQL 8.0 instance running locally (or via `docker-compose up -d`)
- All JARs in `lib/` (crawler4j, httpclient, mysql-connector, etc.)

### Local Build & Run
```bash
# Compile to bin/ folder with classpath including lib/ and src/
javac -d bin -cp "lib/*;src" src/bitvix/indexer/**/**/*.java

# Configure database (optional - uses defaults if not provided):
# Option 1: Create database.properties in the working directory
echo "db.url=jdbc:mysql://localhost:3306/webcrawler" > database.properties
echo "db.username=root" >> database.properties
echo "db.password=" >> database.properties

# Option 2: Set environment variables
set DB_URL=jdbc:mysql://localhost:3306/webcrawler
set DB_USERNAME=root
set DB_PASSWORD=

# Run the main entry point (starts Timer-based scheduler)
java -cp "bin;lib/*" bitvix.indexer.main.Main
```

### Docker
```bash
# Start MySQL container with schema auto-migration
docker-compose up -d

# Check MySQL health
docker-compose logs -f mysql
```

**Database location**: `localhost:3306/webcrawler` (user: `root`, empty password)

## Critical Conventions & Patterns

### 1. **Database Configuration** ✓ IMPROVED
- **NEW**: Externalizable configuration via [src/bitvix/indexer/util/DatabaseConfig.java](src/bitvix/indexer/util/DatabaseConfig.java)
- Credentials can now be provided via:
  - `database.properties` file (in classpath)
  - Environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
  - System properties: `db.url`, `db.username`, `db.password`
  - Hardcoded defaults (for backward compatibility)
- **Legacy**: [src/bitvix/indexer/util/Util.java](src/bitvix/indexer/util/Util.java) now delegates to `DatabaseConfig`
- Connection pattern: Try-with-resources (`try-with-resources`) ensures automatic cleanup (no resource leaks)

### 2. **Error Handling Philosophy** ✓ IMPROVED
- **OLD PATTERN (REMOVED)**: `printError(...); System.exit(0)` called fatal errors, killing the entire process mid-crawl
- **NEW PATTERN**: `logError(context, exception)` logs errors with full stack traces for debugging
- **KEY CHANGE**: Errors no longer force process termination — allows graceful degradation and retry logic
- Callers can implement recovery strategies (e.g., skip failed customer, continue with next)
- See [src/bitvix/indexer/util/DatabaseException.java](src/bitvix/indexer/util/DatabaseException.java) for custom exception handling

### 3. **SQL Injection Prevention** ✓ IMPROVED
- **ALL queries now use `PreparedStatement`** with parameterized bindings
- **OLD PATTERN (REMOVED)**: String concatenation like `"WHERE cus_chp_customer = " + customer.getId()`
- **NEW PATTERN**: Prepared statements with `?` placeholders: `pstm.setInt(1, customer.getId())`
- Examples in refactored [ContentDao.java](src/bitvix/indexer/dao/ContentDao.java): `getDomain()`, `insert()`, `fetchCustomer()`, all now parameterized
- **Result**: SQL injection attacks impossible; automatic escaping of string values

### 4. **Naming Conventions (Hungarian-inspired)**
- Table/column names follow pattern: `tbl/entt + var/int/chp_ + lowercase_name`
- Examples: `cus_chp_customer` (customer ID), `con_var_keyword` (content keyword text)
- **Maintain this** when creating new tables or columns

## Data Flow & Key Files

### Timer & Scheduling
- **[Main.java](src/bitvix/indexer/main/Main.java)**: Runs `Controller()` every 10 seconds (initial delay 1 sec)
- Decision: Timer vs. external scheduler (Quartz, Spring) not addressed; note hard-coded intervals

### Orchestration
- **[Controller.java](src/bitvix/indexer/crawler/Controller.java)**: 
  - Fetches all customers from DB
  - For each customer, fetches related sites
  - Creates `CrawlConfig` per site (max depth, page fetch limit, politeness delay)
  - Starts `CrawlController` with `Crawler` instances

### Crawling & Keyword Matching
- **[Crawler.java](src/bitvix/indexer/crawler/Crawler.java)** (extends `WebCrawler`):
  - `shouldVisit()`: Filters by domain + blacklist + file extensions (blocks .bmp, .gif, .jpg, .png, .css, .js, .rss, .xml)
  - `visit()`: Extracts text/title, checks if any customer keyword matches
  - Calls `ContentDao.insert()` on match
  - Uses `CrawlerData` thread-local holder for per-crawler context

### Data Access
- **[ContentDao.java](src/bitvix/indexer/dao/ContentDao.java)**:
  - `fetchCustomer()`: Loads all customers + related sites into memory
  - `insert()`: Inserts matched content into `entt_content` table
  - No transaction support; auto-commit mode

## Database Schema
Tables in `migration/001_init.sql`:
- `entt_customer`: customers with keywords, situation flag, result count
- `entt_site`: crawl targets (URL, domain, depth, page limits, politeness delay)
- `tbla_customersite`: junction table (customer ↔ site many-to-many)
- `entt_content`: matched content rows (customer, keyword, text, URL, extracted metadata)
- `entt_blacklist`: per-customer blacklisted URLs
- `entt_siteseed`: seed URLs for crawlers
- `entt_contentreaded`: tracks already-fetched URLs per customer (prevents re-indexing)
- `tbla_config`: runtime config (e.g., `cof_int_currentcustomer` for state)

## Known Gaps & Improvement Opportunities
- **No build tool**: Use `pom.xml` or `gradle.build` to manage `lib/` dependencies instead of manual JAR management
- **Connection pooling**: Consider HikariCP or C3P0 for multi-threaded crawling (currently safe with per-call connections)
- **Threading**: `Crawler` threads may not be explicitly joined; potential resource leaks or incomplete indexing
- **API layer**: No REST API for external integrations; consider adding Spring Boot endpoints

## Common Tasks

### Add a new customer-specific field
1. Add column to `entt_customer` in `migration/001_init.sql` (use naming convention)
2. Update `Customer.java` model with getter/setter
3. Update `ContentDao.fetchCustomer()` to populate new field from ResultSet
4. Reference in [Controller.java](src/bitvix/indexer/crawler/Controller.java) or [Crawler.java](src/bitvix/indexer/crawler/Crawler.java) as needed

### Change crawl frequency or max pages
- Edit Timer interval in [Main.java](src/bitvix/indexer/main/Main.java) line ~25 (currently 10 sec)
- Edit max pages per site: `CrawlConfig.setMaxPagesToFetch()` in [Controller.java](src/bitvix/indexer/crawler/Controller.java)

### Debug a crawl not indexing content
1. Check `shouldVisit()` logic in [Crawler.java](src/bitvix/indexer/crawler/Crawler.java): domain matching, blacklist, extensions
2. Check keyword matching in `visit()`: case sensitivity, whitespace
3. Verify `ContentDao.insert()` is being called: add debug print before `conn.createStatement()`
4. Check DB connection in [Util.java](src/bitvix/indexer/util/Util.java): port, database name, credentials

## External Dependencies
- **crawler4j** (in `lib/`): CrawlController, WebCrawler, Page, WebURL, HtmlParseData
- **MySQL JDBC driver** (in `lib/`): `java.sql.*` classes
- **Apache HttpClient** (in `lib/`): used by crawler4j for HTTP fetch

Ensure `lib/` contains all required JARs before compilation.
