package dev.danvega.vmeblog.post;

import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface PostRepository extends ListCrudRepository<Post,Integer> {

    Optional<Post> findPostsByUrlContainsIgnoreCase(String slug);

}
