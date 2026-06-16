package tw.teddysoft.aiscrum.test.suite;

import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.springframework.boot.test.context.SpringBootTest;

@Suite
@SuiteDisplayName("Outbox Pattern Tests - PostgreSQL Database")
@SelectClasses({
        OutboxTestSuite.ProfileSetter.class
})
@SelectPackages({
        "tw.teddysoft.aiscrum.product"
})
@ExcludeClassNamePatterns(".*ControllerTest")
public class OutboxTestSuite {

    @SpringBootTest
    public static class ProfileSetter {
        static {
            System.setProperty("spring.profiles.active", "test-outbox");
            System.setProperty("spring.datasource.url", "jdbc:postgresql://localhost:5800/board?currentSchema=message_store");
            System.setProperty("spring.datasource.username", "postgres");
            System.setProperty("spring.datasource.password", "root");
            System.setProperty("spring.jpa.hibernate.ddl-auto", "update");
            System.setProperty("spring.jpa.properties.hibernate.default_schema", "message_store");
        }

        @Test
        void setProfile() {
        }
    }
}
