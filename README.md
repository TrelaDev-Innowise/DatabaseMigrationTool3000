
# Database Migration Tool 3000

A tool for controlled database migration for PostgreSQL, functioning as a Java library or standalone CLI program.  
Inspired by Flyway, it manages database versions through SQL files, ensuring consistency across developers' environments.

## 📋 Requirements
- Java JDK 17
- PostgreSQL database 
- Apache Maven 3.6+

## ⚙️ Installation
1. Clone the repository:
```bash
git clone https://github.com/TrelaDev-Innowise/DatabaseMigrationTool3000.git
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
Migration files must follow a strict naming format and must be in SQL format. **NOTICE THE DOUBLE UNDERSCORE** between the version number and description:

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
Testing repository is available at:[
https://github.com/TrelaDev-Innowise/DatabaseMigrationTool3000Testing](https://github.com/TrelaDev-Innowise/DatabaseMigrationTool3000Testing)

This repository includes example SQL queries for testing the migration tool.


## 🛠️ Technologies Used

The following technologies were used to build and support the functionality of the Database Migration Tool 3000:

- **Java 17**: The primary programming language used for the implementation of the migration tool.
- **PostgreSQL**: A relational database management system (RDBMS) supported by the migration tool for managing database migrations.
- **Maven**: A build automation tool used for managing dependencies and building the project.
- **Picocli**: A Java library used for building command-line interfaces (CLI). It is used to handle the parameters and commands for the standalone version of the migration tool.
- **JUnit 5**: A testing framework for writing and running unit tests, used for testing the functionality of the migration tool.
- **H2 Database**: A lightweight database used in testing scenarios to simulate migrations without requiring an actual PostgreSQL database.
- **Logback**: A logging framework for Java, providing logging capabilities within the migration tool.
- **HikariCP**: A connection pooling library for managing database connections efficiently.



