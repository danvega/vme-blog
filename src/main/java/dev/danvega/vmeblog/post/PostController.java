package dev.danvega.vmeblog.post;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

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

    @GetMapping("/slug/{slug}")
    Optional<Post> findBySlug(@PathVariable String slug) {
        return repository.findPostsByUrlContainsIgnoreCase(slug);
    }

    // REQUESTS TO ADD METHODS TO THE API

    // find all comments for a post

    // find all related blog posts

    // whatever this is
    List<Post> findAllPostsWithCommentsAndRelatedPosts() {
        return null;
    }

}
