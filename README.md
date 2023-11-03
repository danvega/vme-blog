# VMware Explore Code Theater - GraphQL 

In this live coding session we are going to build a GraphQL API using Spring Boot, Java & Maven. We will start
by building a simple Spring Boot application that will load blog posts from a JSON file and store them in a 
Postgres database.

## Agenda

- [ ] Create Spring Boot Project
- [ ] Configure Docker Compose
- [ ] Load Blog Posts from JSON
- [ ] Post Controller
- [ ] GraphQL Schema
- [ ] Custom Scalar Types
- [ ] GraphQL Controller
- [ ] Comments

### Create a new Spring Boot Project

You can use the [Spring Initializr](start.spring.io) to create a new Spring Boot Project. Make sure to include the following dependencies:

- Spring Web
- Spring GraphQL 
- Spring Data JDBC
- PostgreSQL Driver
- Docker Compose
- Spring Boot Actuator
- Spring Boot Devtools

https://start.spring.io/#!type=maven-project&language=java&platformVersion=3.2.0-RC2&packaging=jar&jvmVersion=21&groupId=dev.danvega&artifactId=vmeblog&name=vmeblog&description=VMware%20Explore%20Code%20Theater%20Blog&packageName=dev.danvega.vmeblog&dependencies=web,data-jdbc,postgresql,docker-compose,graphql,actuator,devtools

I like renaming the main application class to `Application.java` 🤷‍♂️

### Configure Docker Compose 

Configure the properties from the Postgres Database and make sure to expose port 5432 on host and container so that we can connect to it from our IDE tools. 

```yaml
services:
  postgres:
    image: 'postgres:latest'
    environment:
      - 'POSTGRES_DB=blog'
      - 'POSTGRES_PASSWORD=secret'
      - 'POSTGRES_USER=user'
    ports:
      - '5432:5432'
```

After this has been updated, start the application and make sure everything is working. At this time you can 
also connect to the database using your favorite database tool. In my example I am using the built-in database tools
in IntelliJ.

### Load Blog Posts from JSON

The first thing we need to do is to create a new file `/src/main/resources/data/posts.json` to hold our posts. You can 
grab some sample data from my blog at https://www.danvega.dev/feed.json. 

Next you will create a new `Post` record to hold the data from the JSON file.

```java
public record Post(
        @Id
        Integer id,
        String title,
        String summary,
        String url,
        @JsonProperty("date_published")
        LocalDateTime datePublished,
        @Version
        Integer version
) {
}
```

And you will need a `PostRepository` to persist the data to the database. 

```java
public interface PostRepository extends ListCrudRepository<Post,Integer> {
    
}
```

Next we will create `Bootstrap.java` which will implement the `CommandLineRunner` interface. This will allow you to 
execute some code after the application has started. In this case we will load the JSON file and save the data to the
database. 

```java
@Component
public class Bootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Bootstrap.class);
    private final ObjectMapper objectMapper;
    private final PostRepository postRepository;

    public Bootstrap(ObjectMapper objectMapper, PostRepository postRepository) {
        this.objectMapper = objectMapper;
        this.postRepository = postRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if(postRepository.count() == 0) {
            try (InputStream inputStream = TypeReference.class.getResourceAsStream("/data/posts.json")) {
                Posts posts = objectMapper.readValue(inputStream, Posts.class);
                log.info("Reading {} posts from JSON data and saving to database.", posts.posts().size());
                postRepository.saveAll(posts.posts());
            } catch (IOException e) {
                throw new RuntimeException("Failed to read JSON data", e);
            }
        }
    }

}
```

you also need a `Posts` record

```java
public record Posts(List<Post> posts) {
}
```
For this to work Spring Data JDBC requires us to set up the schema for the `Post` table. We can do this by creating a
`schema.sql` file in the `/src/main/resources` directory. 

```sql
CREATE TABLE IF NOT EXISTS Post (
  id SERIAL NOT NULL,
  title varchar(255) NOT NULL,
  summary text,
  url varchar(255) NOT NULL,
  date_published timestamp NOT NULL,
  version INT,
  PRIMARY KEY (id)
);
```

For this schema file to get picked up automatically you need to set a property in your `application.properties` file.

```properties
spring.sql.init.mode=always
```

A simple REST controller to point out some of the issues we are going to try and address by moving to GraphQL. 

```java
@RestController
@RequestMapping("/api/posts")
class PostController {

    private final PostRepository repository;

    public PostController(PostRepository repository) {
        this.repository = repository;
    }

    @GetMapping("")
    List<Post> findAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    Optional<Post> findById(@PathVariable Integer id) {
        return repository.findById(id);
    }

    // REQUESTS TO ADD METHODS TO THE API

    // find all comments for a post

    // find all related blog posts

    // whatever this is
    List<Post> findAllPostsWithCommentsAndRelatedPosts() {
        return null;
    }

}
```

### GraphQL Schema

Spring for GraphQL takes a schema first approach to building GraphQL APIs. This means that we will define our schema
using the GraphQL Schema Definition Language (SDL). This is a human-readable format that allows us to define our types,
queries, mutations and subscriptions. 

If you want to learn more about GraphQL Types and Schemas you can read more about it here: https://graphql.org/learn/schema/

```graphql
type Query {
    findAllPosts: [Post]
}

type Post {
    id: ID!
    title: String
    summary: String
    url: String
    datePublished: String
}
```

### Custom Scalar Types 

What happens when you want to use a custom Scalar type?


```graphql
scalar Date @specifiedBy(url:"https://tools.ietf.org/html/rfc3339")

type Post {
    id: ID!
    title: String
    summary: String
    url: String
    datePublished: Date
    comments: [Comment]
}
```

```java
@Configuration
public class GraphQlConfig {

    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(ExtendedScalars.Date);
    }

}
```

### GraphQL Controller

Next we can begin to build out our GraphQL controller. This is where will define our data fetchers and map them to our
schema.

```java
@Controller
public class PostGraphQlController {

    private static final Logger log = LoggerFactory.getLogger(PostGraphQlController.class);
    private final PostRepository postRepository;
    private final RestClient restClient;

    public PostGraphQlController(PostRepository postRepository) {
        this.postRepository = postRepository;
        this.restClient = RestClient.create("https://jsonplaceholder.typicode.com/");
    }

    @SchemaMapping(typeName = "Query", value = "findAllPosts")
    List<Post> findAll() {
        return postRepository.findAll();
    }

    @QueryMapping
    Optional<Post> findPostById(@Argument Integer id) {
        return postRepository.findById(id);
    }

}
```

### Comments 

Next we can cover how to add comments to our GraphQL API. We will start by creating a new `Comment` record.

```java
public record Comment(Integer id, Integer postId, String name, String email, String body) {
}
```

In this case comments are not stored in the database they are fetched from a REST API. You can think of this as an
example where we might be getting data from another service in our organization. This is an opportunity to talk about
the new `RestClient` in Spring Boot 3.2.

In GraphQL Java, the DataFetchingEnvironment provides access to the source (i.e. parent/container) instance of the field. To access this, simply declare a method parameter of the expected target type. For example, to access the Post instance for a comments field, declare a method parameter of type Post.

```java
@SchemaMapping
List<Comment> comments(Post post) {
    log.info("Fetching comments for post '{}'", post.title());
    List<Comment> allComments = restClient.get()
            .uri("/comments")
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

    return allComments.stream().filter(comment -> comment.postId().equals(post.id())).collect(Collectors.toList());
}
```

Next create comment object type in our GraphQL schema. Finally, you can tie this all together by adding a new field to our `Post` type.

```graphql
type Post {
    id: ID!
    title: String
    summary: String
    url: String
    datePublished: Date
    comments: [Comment]
}

type Comment {
    id: ID
    postId: Int
    name: String
    email: String
    body: String
}
```

## GraphQL Queries

Here are some sample queries that you can run in the GraphiQL UI by visiting http://localhost:8080/graphiql

```graphql
query {
  findAllPosts {
    title
    summary
    url 
    datePublished
  }
}
```

```graphql
query {
  findPostById(id: 1) {
    title
    summary
    url 
    datePublished
    comments {
      name
      email
      body
    }
  }
}
```

## About Me

Hello 👋🏻 My name is Dan Vega, Spring Developer Advocate, Husband and #GirlDad based outside of Cleveland OH. I created this website as a place to document my journey as I learn new things and share them with you. I have a real passion for teaching and I hope that one of blog posts, videos or courses helps you solve a problem or learn something new.

- [danvega.dev](https://www.danvega.dev)
- [Twitter](https://twitter.com/therealdanvega)
- [YouTube](https://www.youtube.com/@danvega)
- [LinkedIn](https://www.linkedin.com/in/danvega/)


## Notes

- Have the following tabs open to start
    - danvega.dev
    - GitHub repo with README
    - start.spring.io
- Open app in IntelliJ with Database Connection Setup
- Rename Application
- Mention the graphql inspection report
- Mutations Examples?
- Delete db from Docker
