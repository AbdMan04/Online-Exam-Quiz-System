package com.quizapp.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A clock that tests can set and move forward, so timer rules (R14, R17, R20) can be tested at exact
 * moments such as "one second before the deadline".
 */
public class MutableClock extends Clock {

    private static final ZoneId ZONE = ZoneOffset.UTC;

    private Instant now;

    public MutableClock(LocalDateTime start) {
        setTo(start);
    }

    public final void setTo(LocalDateTime time) {
        now = time.toInstant(ZoneOffset.UTC);
    }

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZONE;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(now, zone);
    }

    @Override
    public Instant instant() {
        return now;
    }
}
