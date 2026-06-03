package tw.teddysoft.aiscrum.test.suite;

import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.springframework.boot.test.context.SpringBootTest;

@Suite
@SuiteDisplayName("In-Memory Tests - Fast Execution")
@SelectClasses({
        InMemoryTestSuite.ProfileSetter.class
})
@SelectPackages({
        "tw.teddysoft.aiscrum.product"
})
@ExcludeClassNamePatterns(".*ControllerTest")
public class InMemoryTestSuite {

    @SpringBootTest
    public static class ProfileSetter {
        static {
            System.setProperty("spring.profiles.active", "test-inmemory");
            System.setProperty("spring.autoconfigure.exclude",
                    "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration");
        }

        @Test
        void setProfile() {
        }
    }
}
