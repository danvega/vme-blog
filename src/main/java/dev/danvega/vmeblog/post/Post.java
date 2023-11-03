package dev.danvega.vmeblog.post;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.danvega.vmeblog.comment.Comment;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;

import java.time.LocalDateTime;
import java.util.List;

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
