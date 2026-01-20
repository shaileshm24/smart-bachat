package com.ametsa.smartbachat.uam;

import com.ametsa.smartbachat.uam.entity.Role;
import com.ametsa.smartbachat.uam.repository.RoleRepository;
import com.ametsa.smartbachat.uam.repository.UserRepository;
import com.ametsa.smartbachat.uam.repository.VerificationTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for integration tests.
 * Provides common setup and utilities.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected VerificationTokenRepository tokenRepository;

    @BeforeEach
    void setUpBase() {
        // Clean up database before each test
        tokenRepository.deleteAll();
        userRepository.deleteAll();
        
        // Ensure default roles exist
        ensureRoleExists(Role.ROLE_USER, "User", "Default user role");
        ensureRoleExists(Role.ROLE_ADMIN, "Admin", "Administrator role");
    }

    private void ensureRoleExists(String name, String displayName, String description) {
        if (!roleRepository.existsByName(name)) {
            Role role = new Role(name, displayName, description);
            role.setIsSystemRole(true);
            roleRepository.save(role);
        }
    }

    protected String asJsonString(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }
}

