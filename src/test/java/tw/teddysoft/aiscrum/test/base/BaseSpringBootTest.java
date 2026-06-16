package tw.teddysoft.aiscrum.test.base;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base class for Use Case and Repository tests that require Spring context.
 * Contract tests do NOT extend this class (they don't need Spring).
 *
 * IMPORTANT: Do NOT add @ActiveProfiles here!
 * Profile switching is controlled by ProfileSetter in TestSuite.
 */
@SpringBootTest
public abstract class BaseSpringBootTest {
    // Intentionally empty - provides Spring Boot test context
}
