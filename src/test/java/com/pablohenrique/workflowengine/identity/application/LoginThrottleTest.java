package com.pablohenrique.workflowengine.identity.application;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class LoginThrottleTest {

    private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");

    private final MutableClock clock = new MutableClock(START);
    private final LoginThrottle throttle = new LoginThrottle(3, Duration.ofMinutes(1), clock);

    private void fail(String origin, int times) {
        for (int i = 0; i < times; i++) {
            throttle.checkAllowed(origin);
            throttle.recordFailure(origin);
        }
    }

    @Test
    void blocksAnOriginAfterTheMaximumFailuresWithinTheWindow() {
        fail("10.0.0.1", 3);

        TooManyLoginAttemptsException exception = catchThrowableOfType(TooManyLoginAttemptsException.class,
                () -> throttle.checkAllowed("10.0.0.1"));
        assertThat(exception.retryAfterSeconds()).isEqualTo(60);
    }

    @Test
    void originsWithoutFailuresAreNeverBlocked() {
        for (int i = 0; i < 50; i++) {
            throttle.checkAllowed("10.0.0.9");
        }
    }

    @Test
    void originsAreLimitedIndependently() {
        fail("10.0.0.1", 3);

        assertThatCode(() -> throttle.checkAllowed("10.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    void failuresExpireAsTheWindowSlides() {
        fail("10.0.0.1", 1);
        clock.advance(Duration.ofSeconds(30));
        fail("10.0.0.1", 2);

        clock.advance(Duration.ofSeconds(31));

        assertThatCode(() -> throttle.checkAllowed("10.0.0.1")).doesNotThrowAnyException();
        throttle.recordFailure("10.0.0.1");
        assertThatThrownBy(() -> throttle.checkAllowed("10.0.0.1")).isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test
    void rejectsInvalidConfiguration() {
        assertThatThrownBy(() -> new LoginThrottle(0, Duration.ofMinutes(1), clock))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
