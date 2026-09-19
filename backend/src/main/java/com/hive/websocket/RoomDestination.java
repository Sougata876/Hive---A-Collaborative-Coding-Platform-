package com.hive.websocket;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RoomDestination {

    private static final Pattern APP_PATTERN =
            Pattern.compile("^/app/room/(\\d+)/(edit|sync|chat|presence|join|leave)$");
    private static final Pattern TOPIC_PATTERN = Pattern.compile("^/topic/room/(\\d+)/(code|chat|presence)$");

    private RoomDestination() {
    }

    public static Match parse(String destination) {
        if (destination == null) {
            return null;
        }
        Matcher appMatcher = APP_PATTERN.matcher(destination);
        if (appMatcher.matches()) {
            return new Match(Long.parseLong(appMatcher.group(1)), appMatcher.group(2), true);
        }
        Matcher topicMatcher = TOPIC_PATTERN.matcher(destination);
        if (topicMatcher.matches()) {
            return new Match(Long.parseLong(topicMatcher.group(1)), topicMatcher.group(2), false);
        }
        return null;
    }

    public record Match(Long roomId, String action, boolean applicationDestination) {
        public boolean requiresEditPermission() {
            return applicationDestination && action.equals("edit");
        }
    }
}
