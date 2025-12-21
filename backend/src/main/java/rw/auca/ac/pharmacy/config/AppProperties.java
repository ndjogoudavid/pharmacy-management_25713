package rw.auca.ac.pharmacy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Security security = new Security();
    private final Frontend frontend = new Frontend();
    private final Auth auth = new Auth();
    private final Seed seed = new Seed();

    @Data
    public static class Security {
        private final Jwt jwt = new Jwt();

        @Data
        public static class Jwt {
            private String secret;
            private long expirationMs;
        }
    }

    @Data
    public static class Frontend {
        private String resetUrl;
    }

    @Data
    public static class Auth {
        private boolean logCodes;
    }

    @Data
    public static class Seed {
        @Name("demo")
        private boolean demo;
        @Name("demo.password")
        private String demoPassword;
    }
}
