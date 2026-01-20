package com.ametsa.smartbachat.uam.service;

import com.ametsa.smartbachat.uam.dto.UserDto;
import com.ametsa.smartbachat.uam.entity.Role;
import com.ametsa.smartbachat.uam.entity.User;
import com.ametsa.smartbachat.uam.repository.RoleRepository;
import com.ametsa.smartbachat.uam.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public UserDto getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toDto(user);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDto updateUser(UUID id, UserDto updateRequest) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updateRequest.getFirstName() != null) {
            user.setFirstName(updateRequest.getFirstName());
        }
        if (updateRequest.getLastName() != null) {
            user.setLastName(updateRequest.getLastName());
        }
        if (updateRequest.getDisplayName() != null) {
            user.setDisplayName(updateRequest.getDisplayName());
        }
        if (updateRequest.getMobileNumber() != null) {
            // Check if mobile is already used by another user
            userRepository.findByMobileNumber(updateRequest.getMobileNumber())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(id)) {
                            throw new RuntimeException("Mobile number already in use");
                        }
                    });
            user.setMobileNumber(updateRequest.getMobileNumber());
        }

        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);
        
        log.info("User updated: {}", id);
        return toDto(user);
    }

    @Transactional
    public UserDto updateUserStatus(UUID id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(status);
        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);

        log.info("User status updated: {} -> {}", id, status);
        return toDto(user);
    }

    @Transactional
    public UserDto assignRole(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        user.addRole(role);
        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);

        log.info("Role {} assigned to user {}", roleName, userId);
        return toDto(user);
    }

    @Transactional
    public UserDto removeRole(UUID userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        user.removeRole(role);
        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);

        log.info("Role {} removed from user {}", roleName, userId);
        return toDto(user);
    }

    private UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setMobileNumber(user.getMobileNumber());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setDisplayName(user.getDisplayName());
        dto.setStatus(user.getStatus());
        dto.setEmailVerified(user.getEmailVerified());
        dto.setMobileVerified(user.getMobileVerified());
        dto.setLastLoginAt(user.getLastLoginAt());
        dto.setCreatedAt(user.getCreatedAt());
        if (user.getProfile() != null) {
            dto.setProfileId(user.getProfile().getId());
        }
        dto.setRoles(user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList()));
        return dto;
    }
}

