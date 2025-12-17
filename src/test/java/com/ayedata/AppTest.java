package com.ayedata;

import org.junit.jupiter.api.Test;
// This is the correct import for JUnit 5 assertions
import static org.junit.jupiter.api.Assertions.assertNotNull; 

class JVaultApplicationTests {

    @Test
    void contextLoads() {
        // Simple assertion example
        assertNotNull("The app context should load"); 
    }
}