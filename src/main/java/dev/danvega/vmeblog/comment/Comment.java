package dev.danvega.vmeblog.comment;

public record Comment(Integer id, Integer postId, String name, String email, String body) {
}
