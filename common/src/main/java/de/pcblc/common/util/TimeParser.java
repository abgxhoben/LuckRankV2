package de.pcblc.common.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeParser {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("(\\d+)(min|[dhwmy])", Pattern.CASE_INSENSITIVE);

    private TimeParser() {
    }

    public static Result parse(String input) {
        if ("-1".equalsIgnoreCase(input)) {
            return Result.permanent();
        }

        String normalized = input.toLowerCase(Locale.ROOT);
        Matcher matcher = TOKEN_PATTERN.matcher(normalized);
        long seconds = 0L;
        int consumed = 0;

        while (matcher.find()) {
            consumed += matcher.group(0).length();
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            if ("d".equals(unit)) {
                seconds += amount * 86400L;
            } else if ("h".equals(unit)) {
                seconds += amount * 3600L;
            } else if ("min".equals(unit)) {
                seconds += amount * 60L;
            } else if ("w".equals(unit)) {
                seconds += amount * 604800L;
            } else if ("m".equals(unit)) {
                seconds += amount * 2628000L;
            } else if ("y".equals(unit)) {
                seconds += amount * 31536000L;
            }
        }

        if (seconds <= 0L || consumed != normalized.length()) {
            return Result.invalid();
        }

        return Result.temporary(seconds);
    }

    public static final class Result {
        private final boolean valid;
        private final boolean permanent;
        private final long seconds;

        private Result(boolean valid, boolean permanent, long seconds) {
            this.valid = valid;
            this.permanent = permanent;
            this.seconds = seconds;
        }

        public static Result invalid() {
            return new Result(false, false, 0L);
        }

        public static Result permanent() {
            return new Result(true, true, 0L);
        }

        public static Result temporary(long seconds) {
            return new Result(true, false, seconds);
        }

        public boolean isValid() {
            return valid;
        }

        public boolean isPermanent() {
            return permanent;
        }

        public long getSeconds() {
            return seconds;
        }

        public String toDisplay() {
            if (permanent) {
                return "Lifetime";
            }

            long remaining = seconds;
            long days = remaining / 86400L;
            remaining %= 86400L;
            long hours = remaining / 3600L;
            remaining %= 3600L;
            long minutes = remaining / 60L;

            StringBuilder builder = new StringBuilder();
            if (days > 0) {
                builder.append(days).append("d ");
            }
            if (hours > 0) {
                builder.append(hours).append("h ");
            }
            if (minutes > 0) {
                builder.append(minutes).append("min");
            }

            String text = builder.toString().trim();
            return text.isEmpty() ? seconds + "s" : text;
        }
    }
}
