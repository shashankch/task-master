package com.taskmaster.task.adapter.in;

import com.taskmaster.shared.dto.ApiResponse;
import com.taskmaster.shared.dto.PageResponse;
import com.taskmaster.task.application.dto.AssignTaskRequest;
import com.taskmaster.task.application.dto.CreateTaskRequest;
import com.taskmaster.task.application.dto.TaskFilterCriteria;
import com.taskmaster.task.application.dto.TaskResponse;
import com.taskmaster.task.application.dto.UpdateTaskRequest;
import com.taskmaster.task.application.dto.UpdateTaskStatusRequest;
import com.taskmaster.task.application.service.TaskService;
import com.taskmaster.task.domain.model.TaskPriority;
import com.taskmaster.task.domain.model.TaskStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
@Tag(name = "Tasks", description = "Task CRUD, status transitions, assignment, searching, and filtering endpoints")
@SecurityRequirement(name = "BearerAuth")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    @Operation(summary = "Create a new task")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody CreateTaskRequest request
    ) {
        UUID creatorId = UUID.fromString(jwt.getSubject());
        TaskResponse response = taskService.createTask(creatorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(@PathVariable("id") UUID id) {
        TaskResponse response = taskService.getTaskById(id);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @GetMapping
    @Operation(summary = "Search and filter tasks with multi-field sorting and pagination")
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> searchTasks(
        @RequestParam(value = "status", required = false) TaskStatus status,
        @RequestParam(value = "priority", required = false) TaskPriority priority,
        @RequestParam(value = "assigneeId", required = false) UUID assigneeId,
        @RequestParam(value = "creatorId", required = false) UUID creatorId,
        @RequestParam(value = "teamId", required = false) UUID teamId,
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "dueDateFrom", required = false) Instant dueDateFrom,
        @RequestParam(value = "dueDateTo", required = false) Instant dueDateTo,
        @RequestParam(value = "label", required = false) String label,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        TaskFilterCriteria criteria = new TaskFilterCriteria(
            status,
            priority,
            assigneeId,
            creatorId,
            teamId,
            search,
            dueDateFrom,
            dueDateTo,
            label,
            false
        );

        PageResponse<TaskResponse> response = taskService.searchTasks(criteria, pageable);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task details")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("id") UUID id,
        @Valid @RequestBody UpdateTaskRequest request
    ) {
        UUID updaterId = UUID.fromString(jwt.getSubject());
        TaskResponse response = taskService.updateTask(id, updaterId, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Transition task lifecycle status")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskStatus(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("id") UUID id,
        @Valid @RequestBody UpdateTaskStatusRequest request
    ) {
        UUID changerId = UUID.fromString(jwt.getSubject());
        TaskResponse response = taskService.updateTaskStatus(id, changerId, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Assign or unassign task")
    public ResponseEntity<ApiResponse<TaskResponse>> assignTask(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("id") UUID id,
        @RequestBody AssignTaskRequest request
    ) {
        UUID assignerId = UUID.fromString(jwt.getSubject());
        TaskResponse response = taskService.assignTask(id, assignerId, request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete a task")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteTask(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable("id") UUID id
    ) {
        UUID deleterId = UUID.fromString(jwt.getSubject());
        taskService.deleteTask(id, deleterId);
        return ResponseEntity.ok(ApiResponse.of(Map.of("message", "Task successfully deleted")));
    }
}
