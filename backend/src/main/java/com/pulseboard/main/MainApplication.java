package com.pulseboard.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// Domain packages (user/, expense/, common/, ...) live under com.pulseboard, a sibling of
// this class's com.pulseboard.main package, so component/entity/repository scanning must be
// pointed at the shared "com.pulseboard" root explicitly rather than relying on the default
// (which would only scan com.pulseboard.main and miss every domain package).
// UserDetailsServiceAutoConfiguration is excluded because auth is handled entirely by our own
// JWT filter chain (SecurityConfig) - without this, Spring Security auto-generates an unused
// in-memory dev user/password on every startup.
@SpringBootApplication(scanBasePackages = "com.pulseboard", exclude = UserDetailsServiceAutoConfiguration.class)
@EntityScan("com.pulseboard")
@EnableJpaRepositories("com.pulseboard")
public class MainApplication {

	public static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}

}
