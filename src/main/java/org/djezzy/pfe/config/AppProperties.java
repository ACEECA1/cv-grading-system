package org.djezzy.pfe.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private int verificationCodeExpiryMinutes;
    private Ocr ocr = new Ocr();
    private String fileUploadDir;
    private String companyName;
    private List<String> frontendUrls;
    private Callback callback = new Callback();
    private N8n n8n = new N8n();
    private Automation automation = new Automation();
    private Logging logging = new Logging();
    private Openrouter openrouter = new Openrouter();
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
    public static class Automation {
        private boolean useN8n = true;
    }

    @Getter
    @Setter
    public static class Callback {
        private String apiKey;
    }

    @Getter
    @Setter
    public static class Logging {
        private boolean showWorkflow = false;
    }

    @Getter
    @Setter
    public static class Openrouter {
        private String apiKey;
        private String url;
        private String model;
        private String systemPrompt;
        private String repairSystemPrompt;
        private boolean logPayloads;
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
