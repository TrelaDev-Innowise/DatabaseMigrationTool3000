
# Database Migration Tool 3000

A tool for controlled database migration for PostgreSQL, functioning as a Java library or standalone CLI program.  
Inspired by Flyway, it manages database versions through SQL files, ensuring consistency across developers' environments.

## 📋 Requirements
- Java JDK 17+
- PostgreSQL database (version 9.6 or newer)
- Apache Maven 3.6+

## ⚙️ Installation
1. Clone the repository:
```bash
git clone https://github.com/your-repo/DatabaseMigrationTool3000.git
```

2. Build the project and install it to your local Maven repository:
```bash
cd path/to/project  # e.g. cd C:\Projects\DatabaseMigrationTool3000
mvn clean install
```

The installed artifact will be located in:
```bash
~/.m2/repository/com/trela/DatabaseMigrationTool3000/1.0-SNAPSHOT/
```

## 🛠️ Usage
1. As a Java Library
Add the dependency to your Maven project:

```xml
<dependency>
    <groupId>com.trela</groupId>
    <artifactId>DatabaseMigrationTool3000</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Example implementation:

```java
public class Main {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/mydb";
        String user = "admin";
        String password = "1234";
        
        DatabaseMigrationTool3000 migrator = new DatabaseMigrationTool3000(url, user, password);
        migrator.runMigrations("src/main/resources/migrations");
    }
}
```

2. As Standalone CLI
Run the built JAR file with parameters:

```bash
java -jar DatabaseMigrationTool3000-1.0-SNAPSHOT.jar   --url=jdbc:postgresql://localhost:5432/mydb   --username=admin   --password=secret   --directory=/path/to/migrations
```

## 📂 Migration File Structure
Migration files must follow a strict naming format:

```bash
V<VERSION>__<DESCRIPTION>.sql
```

Example:

```bash
migrations/
├── V1__init_database.sql
├── V2__add_users_table.sql
└── V3__create_indexes.sql
```

Validation Requirements:
- Versions must be consecutive integers (V1 → V2 → V3)
- Each migration must have a unique version
- Modifying existing migrations is prohibited

## 🔒 Security Mechanisms
- Checksums: Each migration is verified by an MD5 checksum
- Transaction Locks: EXCLUSIVE LOCK on the migration table during operations
- Migration History: All changes are logged in the migration_history table
- Error Handling: Automatic rollback on failed migrations

## 🚨 Supported Errors
| Error Code        | Cause                         | Solution                                  |
|-------------------|-------------------------------|-------------------------------------------|
| MigrationVersionGap | Gap in version numbering      | Add the missing migration V(n+1)          |
| InvalidChecksum   | Modification of an existing migration | Restore the original file version       |
| ConcurrentMigration | Parallel migration from another source | Wait for the process to complete    |
| InvalidFileNaming | Incorrect file name format    | Adjust to the format VX__desc.sql         |

## 🧪 Testing
Testing repository is available at:
[https://github.com/your-repo/MigrationTool3000Testing](https://github.com/your-repo/MigrationTool3000Testing)

Example test database:

```java
// Connecting to local PostgreSQL instance
String testUrl = "jdbc:postgresql://localhost:5432/testdb";
String testUser = "tester";
String testPass = "test123";
```

## 💡 Best Practices
- Use environment variables for sensitive data:

```bash
--password=${DB_PASSWORD}
```

- Always migrate "up" - never modify executed migrations
- Regularly export the database schema as a backup
- Run migrations within manual transactions

## 📄 License
MIT License - full text in LICENSE file
