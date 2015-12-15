# OSIAM resource server

## 2.5 - 2015-12-15

### Features

- Use JDBC connection pooling

    By default the pool has a size of 10 and a timeout of 30s to acquire a connection.
    These settings can be changed with the following configuration properties:

    - `org.osiam.resource-server.db.maximum-pool-size`
    - `org.osiam.resource-server.db.connection-timeout-ms`

- Populate the `type` field of a `Group`'s members

    Members of a `Group` have their `type` field set to either `User` or `Group`.

- Make number of parallel connections to the auth-server configurable

    The default is 40 and can be changed with the following configuration property:

    - `org.osiam.auth-server.connector.max-connections`

- Make timeouts of connections to auth-server configurable

    By default the read timeout is set to 10000ms and the connect timeout to 5000ms.
    These settings can be changed with the following configuration properties:

    - `org.osiam.auth-server.connector.read-timeout-ms`
    - `org.osiam.auth-server.connector.connect-timeout-ms`

### Changes

- Increase default timeouts for connections to auth-server

    By default the read timeout is set to 10000ms and the connect timeout to 5000ms.

- Increase default maximum number of parallel connections to auth-server

    The default is 40.

- Switch to Spring Boot

- Refactor database schema

    **Note:** Some fields in table `scim_extension_field` have been renamed:

    - `extension_internal_id` becomes `extension`;
    - `is_required` becomes `required`;

    Update your SQL scripts, if you add SCIM 2 extensions via direct database
    manipulation.

- Produce a meaningful log message and respond with `503 TEMPORARILY UNAVAILABLE`
  instead of `409 CONFLICT` if the auth-server cannot be reached to validate or
  revoke an access token.

- All invalid search queries now respond with a `400 BAD REQUEST` instead of
  `409 CONFLICT` status code.

- Respond with `401 UNAUTHORIZED` when revoking or validating an access token
  fails because of invalid access token.

- Remove configuration property `org.osiam.resource-server.db.dialect`

- Remove self written profiling solution since we now use the [Metrics](https://github.com/dropwizard/metrics)
  framework. This removes the configuration property `org.osiam.resource-server.profiling`

- Make the generated errors SCIM compliant

    Error responses look like this according to [Scim 2](http://tools.ietf.org/html/rfc7644):

        {
          "schemas": ["urn:ietf:params:scim:api:messages:2.0:Error"],
          "detail": "Resource 2819c223-7f76-453a-919d-413861904646 not found",
          "status": "404"
        }


### Fixes

- Only set `UserEntity#active` if value is not null

    Prevents a NPE when storing users that have no value for the `active` field.

- Use correct schema for Scim resources

    Affected resources and the changes are:

    - `User`: `urn:scim:schemas:core:2.0:User` becomes `urn:ietf:params:scim:schemas:core:2.0:User`
    - `Group`: `urn:scim:schemas:core:2.0:Group` becomes `urn:ietf:params:scim:schemas:core:2.0:Group`
    - `ListResponse`: `urn:scim:schemas:core:2.0:User`/`urn:scim:schemas:core:2.0:Group` becomes `urn:ietf:params:scim:api:messages:2.0:ListResponse`
    - `ServiceProviderConfig`: `urn:scim:schemas:core:2.0:ServiceProviderConfig` becomes `urn:ietf:params:scim:schemas:core:2.0:ServiceProviderConfig`

### Updates

- OSIAM connector4java 1.8
- MySQL JDBC driver 5.1.37
- PostgreSQL JDBC driver 9.4-1205
- AspectJ 1.8.7
- Metrics Spring Integration 3.1.2

## 2.4

Skipped to synchronize OSIAM main version with versions of the core servers

## 2.3 - 2015-10-09

Revoked, see 2.5

## 2.2 - 2015-06-18

### Changes

- Bump connector to make use of more concurrent HTTP connections

## 2.1 - 2015-06-02

### Features

- Support for new `ME` scope
- Support for new `ADMIN` scope

### Fixes

- Secure search endpoint on `/`
- PostalCode should not be retrieved as literal `null` string when not set

### Other

- resource-server now lives in its own Git repo
- Changed artifact id from `osiam-resource-server` to `resource-server`

## 2.0 - 2015-04-29

**Breaking changes!**

This release introduces breaking changes, due to the introduction of automatic
database schema updates powered by Flyway. See the
[migration notes](docs/Migration.md#from-13x-to-20) for further details.

- [feature] Support automatic database migrations
- [feature] create JAR containing the classes of app
- [fix] lower constraint index lengths for MySQL
- [fix] replace Windows line endings with Unix ones in SQL scripts
- [change] decrease default verbosity
- [change] bump dependency versions
- [docs] move documentation from Wiki to repo
- [docs] rename file RELEASE.NOTES to CHANGELOG.md

## 1.3 - 2014-10-17

- [fix] Infinite recursion when filtering or sorting by x509certivicates.value
- [fix] Sorting by name sub-attribute breaks the result list

    For a detailed description and migration see:
    https://github.com/osiam/server/wiki/Migration#from-12-to-13

## 1.2 - 2014-09-30

- [feature] Introduced an interface to get the extension definitions (/osiam/extension-definition)

## 1.1 - 2014-09-19

- [feature] support for mysql as database
- [enhancement] Force UTF-8 encoding of requests and responses
- [enhancement] better error message on search
  When searching for resources and forgetting the surrounding double quotes for
  values, a non-understandable error message was responded. the error message
  was changed to explicitly tell that the error occurred due to missing
  double quotes.
- [enhancement] updated dependencies: Spring 4.1.0, Spring Security 3.2.5,
  Spring Metrics 3.0.2, Jackson 2.4.2, Hibernate 4.3.6, AspectJ 1.8.2,
  Joda Time 2.4, Joda Convert 1.7, Apache Commons Logging 1.2, Guava 18.0,
  Postgres JDBC Driver 9.3-1102-jdbc41
