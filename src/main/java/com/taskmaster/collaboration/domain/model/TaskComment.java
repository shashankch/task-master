package com.taskmaster.collaboration.domain.model;

import com.taskmaster.shared.domain.BaseEntity;
import com.taskmaster.task.domain.model.Task;
import com.taskmaster.user.domain.model.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task_comments")
public class TaskComment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TaskComment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL)
    @OrderBy("createdAt ASC")
    private List<TaskComment> replies = new ArrayList<>();

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public TaskComment() {
    }

    public TaskComment(Task task, User author, TaskComment parentComment, String content) {
        this.task = task;
        this.author = author;
        this.parentComment = parentComment;
        this.content = content != null ? content.trim() : "";
    }

    public void editContent(String newContent) {
        if (newContent != null && !newContent.isBlank()) {
            this.content = newContent.trim();
        }
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
        this.content = "[Comment deleted]";
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public TaskComment getParentComment() {
        return parentComment;
    }

    public void setParentComment(TaskComment parentComment) {
        this.parentComment = parentComment;
    }

    public List<TaskComment> getReplies() {
        return replies;
    }

    public void setReplies(List<TaskComment> replies) {
        this.replies = replies;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
