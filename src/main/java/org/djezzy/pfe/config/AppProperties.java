package org.djezzy.pfe.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private int verificationCodeExpiryMinutes;
    private Ocr ocr = new Ocr();
    private String fileUploadDir;
    private Callback callback = new Callback();
    private N8n n8n = new N8n();
    private Admin admin = new Admin();

    @Getter
    @Setter
    public static class Ocr {
        private String url;
        private String apiKey;
    }

    @Getter
    @Setter
    public static class N8n {
        private String structuredJdUrl;
        private String evaluationUrl;
    }

    @Getter
    @Setter
    public static class Callback {
        private String apiKey;
    }

    @Getter
    @Setter
    public static class Admin {
        private String username;
        private String firstName;
        private String lastName;
        private String email;
        private String password;
    }
}
