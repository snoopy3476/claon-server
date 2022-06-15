package coLaon.ClaonBack.config;

import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@RequiredArgsConstructor
@ConfigurationProperties("emailsender")
public class EmailSenderConfig {

    private final JavaMailSender emailSender;
    
    private String fromAddress;
}
