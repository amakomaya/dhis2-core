# Extend platform POC

## Goal

Build a standalone jar which can be dropped into Tomcat to extend DHIS.

## Steps to build standalone jar
- create new module [extend-platform] in DHIS parent pom
- it would look something like this:
```xml
<modules>
    <module>dhis-api</module>
    <module>dhis-services</module>
    <module>dhis-support</module>
    <module>dhis-web-api</module>
    <module>dhis-test-web-api</module>
    <module>dhis-web-embedded-jetty</module>
    <module>dhis-test-coverage</module>
    <module>dhis-test-integration</module>
    <module>extend-platform</module>
  </modules>
```
- set packaging as jar
```xml
<packaging>jar</packaging>
```

- add the required DHIS dependencies (in this example we only need the API (service interfaces are defined here) and Spring web to be able to retrieve data from the database)
```xml
  <dependencies>
    <dependency>
      <groupId>org.hisp.dhis</groupId>
      <artifactId>dhis-api</artifactId>
      <version>${dhis2.version}</version>
    </dependency>
    <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-web</artifactId>
      <version>${spring.version}</version>
    </dependency>
  </dependencies>
```

- to develop and test with embedded jetty, add the new module into its dependencies
```xml
    <dependency>
      <groupId>org.hisp.dhis</groupId>
      <artifactId>extend-platform</artifactId>
      <version>2.41-SNAPSHOT</version>
      <type>jar</type>
    </dependency>
```


- When happy with development, build the new module as a jar running this command in the new module directory
`mvn install`

- to test with tomcat
    - add the dhis war to webapps as usual
    - drop the new jar into `{tomcat-home}/libexec/webapps/dhis/WEB-INF/lib`

## Notes
- behaviour may differ than in dev mode
  - e.g. returning metadata with all fields through the API whereas normal controllers only return the `id` & `displayName` by default