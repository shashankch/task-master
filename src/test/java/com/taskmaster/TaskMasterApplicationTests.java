package com.taskmaster;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TaskMasterApplicationTests {

    @Test
    @DisplayName("Application context loads successfully")
    void contextLoads() {
        // Verifies Spring application context bootstrapping
    }
}
