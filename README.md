# SCOW 

A simple code generator (as a maven plugin) for java DTOs and DAOs from raw sql.

## What does it do?

Scow takes a directory of files containing SQL `select` statements and turns 
those into .java source code files.

The generated DTO classes contain typed fields for every column in the sql file
and also DAO methods for querying the data using 
[sql2o](https://www.sql2o.org/).

The DAO methods are built using the name and type of the named queries, which 
are determined by the jdbc resultSet metadata. This means that the methods do 
have typed parameters and that a database that is targeted for the SQL queries 
must be accessible during code generation.      

There are also "modifiers" which enable different selection modifiers to be 
applied for a single query, using the `---: modifierName` syntax.

## Example

file `queries/my/app/sql/User.sql`
```sql
SELECT
    *
FROM
    user
---: allOrdered
ORDER BY name
---: getById
WHERE
    id = :id
---: matchByName
WHERE
    name LIKE :name
```

would emit a file `src/gen/java/my/app/sql/UserDto.java`

with the contents

```java
package my.app.sql;

// import... 

/**
 * see <a href="file:///path/to/queries/my/app/sql/User.sql">User.sql</a> */
public final class UserDto {
    public Integer id;

    public String name;

    public String password;

    public Timestamp created;

    public static class allOrdered {
        private static final String QUERY = "SELECT\n"
                + "    *\n"
                + "FROM\n"
                + "    users\n"
                + "\n"
                + "ORDER BY name";

        public static Query query(Connection conn) {
            return conn.createQuery(QUERY)
                    ;
        }

        public static List<UserDto> fetchAll(Connection conn) {
            return conn.createQuery(QUERY)
                    .executeAndFetch(UserDto.class);
        }

        public static UserDto fetch(Connection conn) {
            return conn.createQuery(QUERY)
                    .executeAndFetchFirst(UserDto.class);
        }
    }

    public static class getById {
        private static final String QUERY = "SELECT\n"
                + "    *\n"
                + "FROM\n"
                + "    users\n"
                + "\n"
                + "WHERE id=:userId";

        public static Query query(Connection conn, Integer userId) {
            return conn.createQuery(QUERY)
                    .addParameter("userId", userId)
                    ;
        }

        public static List<UserDto> fetchAll(Connection conn, Integer userId) {
            return conn.createQuery(QUERY)
                    .addParameter("userId", userId)
                    .executeAndFetch(UserDto.class);
        }

        public static UserDto fetch(Connection conn, Integer userId) {
            return conn.createQuery(QUERY)
                    .addParameter("userId", userId)
                    .executeAndFetchFirst(UserDto.class);
        }
    }

    public static class matchByName {
        private static final String QUERY = "SELECT\n"
                + "    *\n"
                + "FROM\n"
                + "    users\n"
                + "\n"
                + "WHERE name LIKE :name";

        public static Query query(Connection conn, String name) {
            return conn.createQuery(QUERY)
                    .addParameter("name", name)
                    ;
        }

        public static List<UserDto> fetchAll(Connection conn, String name) {
            return conn.createQuery(QUERY)
                    .addParameter("name", name)
                    .executeAndFetch(UserDto.class);
        }

        public static UserDto fetch(Connection conn, String name) {
            return conn.createQuery(QUERY)
                    .addParameter("name", name)
                    .executeAndFetchFirst(UserDto.class);
        }
    }
}

```

In your application, usage could look like this:

```java

    try (Connection con = sql2o.open()) {
        // fetch all rows
        List<UserDto> users = UserDto.matchByName.fetchAll(con, "john");

        // or fetch single row
        UserDto adminUser = UserDto.getById.fetch(con, 1);
    }

```

## Notes

This is all 

* very limited 
* pretty much work in progress

Disclaimer: Don't use this in production. Or do.

## Limitations  

* For some reason, jdbc's result set metadata doesn't return usable data for 
  sqlite (Objects only).



## Dependencies

* Scow outputs code that uses sql2o


## TODO

how to use - maven plugin configuration 
 
 ## License

This software is published under the [MIT License](LICENSE.txt)