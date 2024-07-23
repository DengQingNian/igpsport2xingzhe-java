package com.dengqn.igps2xingzhe.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author dengqn
 * @since 2024/7/20 11:23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "user.giant")
public class Giant implements Serializable {
    @Serial
    private static final long serialVersionUID = 1996804203171936839L;

    private Boolean enable;
    private String username;
    private String password;
}
