package com.taskmaster.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.taskmaster.shared.exception.ResourceNotFoundException;
import com.taskmaster.user.application.dto.UpdateProfileRequest;
import com.taskmaster.user.application.dto.UserResponse;
import com.taskmaster.user.application.mapper.UserMapper;
import com.taskmaster.user.domain.model.Role;
import com.taskmaster.user.domain.model.User;
import com.taskmaster.user.domain.port.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserMapper userMapper;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
        userService = new UserService(userRepository, userMapper);
    }

    @Test
    @DisplayName("Should retrieve profile for existing user")
    void getProfile_WhenUserExists_ShouldReturnProfile() {
        UUID userId = UUID.randomUUID();
        User user = new User("user@example.com", "username", "pw", "Original Name", Role.USER);
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse response = userService.getProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.displayName()).isEqualTo("Original Name");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when user does not exist")
    void getProfile_WhenUserNotFound_ShouldThrow() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(userId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User with id '" + userId + "' not found");
    }

    @Test
    @DisplayName("Should update user profile fields")
    void updateProfile_WhenValid_ShouldUpdateAndReturn() {
        UUID userId = UUID.randomUUID();
        User user = new User("user@example.com", "username", "pw", "Original Name", Role.USER);
        user.setId(userId);

        UpdateProfileRequest request = new UpdateProfileRequest("New Display Name", "https://example.com/avatar.png");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.updateProfile(userId, request);

        assertThat(response.displayName()).isEqualTo("New Display Name");
        assertThat(response.avatarUrl()).isEqualTo("https://example.com/avatar.png");
    }
}
