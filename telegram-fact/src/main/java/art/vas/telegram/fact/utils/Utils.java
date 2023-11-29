package art.vas.telegram.fact.utils;

import jakarta.persistence.NonUniqueResultException;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;

public class Utils {

    public static <V> List<V> sublist(List<V> list, int max) {
        return ListUtils.emptyIfNull(list).stream().limit(max).collect(Collectors.toList());
    }

    public static String toCyrillic(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '+') {
                sb.append(' ');
                continue;
            }
            Character.UnicodeBlock of = Character.UnicodeBlock.of(c);
            if (Character.UnicodeBlock.CYRILLIC.equals(of)) {
                sb.append(c);
            }
            if (Character.UnicodeBlock.BASIC_LATIN.equals(of)) {
                sb.append(c);
            }
        }
        if (sb.isEmpty()) return text;
        return sb.toString();
    }

    public static String underscore2camel(String text) {
        return Objects.isNull(text) ? null : LOWER_UNDERSCORE.to(LOWER_CAMEL, text);
    }

    public static String camel2underscore(String text) {
        return Objects.isNull(text) ? null : LOWER_CAMEL.to(LOWER_UNDERSCORE, text);
    }

    @Nullable
    public static <V> V safetyGet(Supplier<V, Exception> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return null;
        }
    }

    @Nullable
    public static <V> V safetyGet(Supplier<V, Exception>... supplier) {
        for (Supplier<V, Exception> s : supplier) {
            try {
                return s.get();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    @FunctionalInterface
    public interface Supplier<T, E extends Throwable> {
        T get() throws E;
    }
}
