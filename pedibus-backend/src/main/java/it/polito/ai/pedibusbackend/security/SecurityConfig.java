package it.polito.ai.pedibusbackend.security;

import it.polito.ai.pedibusbackend.security.jwt.JwtConfigurer;
import it.polito.ai.pedibusbackend.security.jwt.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private UserDetailsService userDetailsService;

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .httpBasic().disable()
            .csrf().disable()
            .cors().and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .headers().frameOptions().disable() // Remove before submit (allows h2-console access)
            .and()
                .authorizeRequests()
                .antMatchers("/stomp-websocket", "/login", "/register/*", "/recover", "/recover/*", "/stylesheets/*").permitAll()
                .antMatchers("/users").hasAnyRole("SYSTEM-ADMIN", "ADMIN")
                .antMatchers("/users/*/lines", "/users/*/lines/*").hasAnyRole("SYSTEM-ADMIN", "ADMIN")
                .antMatchers(HttpMethod.POST,"/rides").hasAnyRole("SYSTEM-ADMIN", "ADMIN")
                .antMatchers(HttpMethod.PUT,"/rides/*").hasAnyRole("SYSTEM-ADMIN", "ADMIN")
                .antMatchers(HttpMethod.DELETE,"/rides/*").hasAnyRole("SYSTEM-ADMIN", "ADMIN")
                .antMatchers(HttpMethod.GET,"/rides/*/availabilities").hasAnyRole("SYSTEM-ADMIN", "ADMIN")
                .anyRequest().authenticated()
        .and().logout()
        .and().exceptionHandling()
                .authenticationEntryPoint(entryPoint())
        .and().apply(new JwtConfigurer(jwtTokenProvider));

    }

    @Bean
    public AuthenticationEntryPoint entryPoint(){
        return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedMethods("GET", "HEAD", "POST", "DELETE", "PUT");
            }
        };
    }
}
