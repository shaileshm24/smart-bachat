package com.ametsa.smartbachat.uam.service;

import com.ametsa.smartbachat.uam.TestUtils;
import com.ametsa.smartbachat.uam.dto.UserDto;
import com.ametsa.smartbachat.uam.entity.Role;
import com.ametsa.smartbachat.uam.entity.User;
import com.ametsa.smartbachat.uam.repository.RoleRepository;
import com.ametsa.smartbachat.uam.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        testUser = TestUtils.createTestUserWithProfile("test@example.com");
        userRole = TestUtils.createUserRole();
        adminRole = TestUtils.createAdminRole();
        testUser.addRole(userRole);
    }

    @Nested
    @DisplayName("Get User")
    class GetUser {

        @Test
        @DisplayName("Should get user by ID")
        void shouldGetUserById() {
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

            UserDto result = userService.getUserById(testUser.getId());

            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getFirstName()).isEqualTo("Test");
            assertThat(result.getLastName()).isEqualTo("User");
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            UUID nonExistentId = UUID.randomUUID();
            when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserById(nonExistentId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }

        @Test
        @DisplayName("Should get all users")
        void shouldGetAllUsers() {
            User user2 = TestUtils.createTestUser("user2@example.com", "User", "Two");
            when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));

            List<UserDto> result = userService.getAllUsers();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getEmail()).isEqualTo("test@example.com");
            assertThat(result.get(1).getEmail()).isEqualTo("user2@example.com");
        }
    }

    @Nested
    @DisplayName("Update User")
    class UpdateUser {

        @Test
        @DisplayName("Should update user first name")
        void shouldUpdateUserFirstName() {
            UserDto updateRequest = new UserDto();
            updateRequest.setFirstName("Updated");

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserDto result = userService.updateUser(testUser.getId(), updateRequest);

            assertThat(result.getFirstName()).isEqualTo("Updated");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should update user last name")
        void shouldUpdateUserLastName() {
            UserDto updateRequest = new UserDto();
            updateRequest.setLastName("NewLastName");

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserDto result = userService.updateUser(testUser.getId(), updateRequest);

            assertThat(result.getLastName()).isEqualTo("NewLastName");
        }

        @Test
        @DisplayName("Should update mobile number")
        void shouldUpdateMobileNumber() {
            UserDto updateRequest = new UserDto();
            updateRequest.setMobileNumber("+919876543210");

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.empty());
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserDto result = userService.updateUser(testUser.getId(), updateRequest);

            assertThat(result.getMobileNumber()).isEqualTo("+919876543210");
        }

        @Test
        @DisplayName("Should throw exception when mobile already in use")
        void shouldThrowExceptionWhenMobileInUse() {
            User otherUser = TestUtils.createTestUser("other@example.com", "Other", "User");
            UserDto updateRequest = new UserDto();
            updateRequest.setMobileNumber("+919876543210");

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(userRepository.findByMobileNumber("+919876543210")).thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> userService.updateUser(testUser.getId(), updateRequest))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Mobile number already in use");
        }
    }

    @Nested
    @DisplayName("Update User Status")
    class UpdateUserStatus {

        @Test
        @DisplayName("Should update user status to INACTIVE")
        void shouldUpdateUserStatusToInactive() {
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserDto result = userService.updateUserStatus(testUser.getId(), "INACTIVE");

            assertThat(result.getStatus()).isEqualTo("INACTIVE");
        }

        @Test
        @DisplayName("Should update user status to SUSPENDED")
        void shouldUpdateUserStatusToSuspended() {
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserDto result = userService.updateUserStatus(testUser.getId(), "SUSPENDED");

            assertThat(result.getStatus()).isEqualTo("SUSPENDED");
        }
    }

    @Nested
    @DisplayName("Role Management")
    class RoleManagement {

        @Test
        @DisplayName("Should assign role to user")
        void shouldAssignRoleToUser() {
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(roleRepository.findByName(Role.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserDto result = userService.assignRole(testUser.getId(), Role.ROLE_ADMIN);

            assertThat(result.getRoles()).contains(Role.ROLE_ADMIN);
        }

        @Test
        @DisplayName("Should throw exception when role not found")
        void shouldThrowExceptionWhenRoleNotFound() {
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(roleRepository.findByName("ROLE_NONEXISTENT")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.assignRole(testUser.getId(), "ROLE_NONEXISTENT"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Role not found: ROLE_NONEXISTENT");
        }

        @Test
        @DisplayName("Should remove role from user")
        void shouldRemoveRoleFromUser() {
            testUser.addRole(adminRole);
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(roleRepository.findByName(Role.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            UserDto result = userService.removeRole(testUser.getId(), Role.ROLE_ADMIN);

            assertThat(result.getRoles()).doesNotContain(Role.ROLE_ADMIN);
        }

        @Test
        @DisplayName("Should throw exception when removing non-existent role")
        void shouldThrowExceptionWhenRemovingNonExistentRole() {
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(roleRepository.findByName("ROLE_NONEXISTENT")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.removeRole(testUser.getId(), "ROLE_NONEXISTENT"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Role not found: ROLE_NONEXISTENT");
        }
    }

    @Nested
    @DisplayName("User DTO Mapping")
    class UserDtoMapping {

        @Test
        @DisplayName("Should map all user fields to DTO")
        void shouldMapAllUserFieldsToDto() {
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

            UserDto result = userService.getUserById(testUser.getId());

            assertThat(result.getId()).isEqualTo(testUser.getId());
            assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
            assertThat(result.getFirstName()).isEqualTo(testUser.getFirstName());
            assertThat(result.getLastName()).isEqualTo(testUser.getLastName());
            assertThat(result.getStatus()).isEqualTo(testUser.getStatus());
            assertThat(result.getEmailVerified()).isEqualTo(testUser.getEmailVerified());
            assertThat(result.getProfileId()).isEqualTo(testUser.getProfile().getId());
            assertThat(result.getRoles()).contains(Role.ROLE_USER);
        }
    }
}

