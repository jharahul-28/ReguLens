package com.rj.ReguLens;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ReguLensApplicationTests {

    @MockitoBean
    private DataSource dataSource;

    @Test
    void contextLoads() {
        assertTrue(true);
    }
}
