package tw.teddysoft.aiscrum.common.entity;

import java.time.Instant;

public class DateProvider {

    private static Instant fixedInstant = null;

    public static Instant now() {
        if (fixedInstant != null) {
            return fixedInstant;
        }
        return Instant.now();
    }

    public static void useFixedInstant(Instant instant) {
        fixedInstant = instant;
    }

    public static void useSystemTime() {
        fixedInstant = null;
    }
}
