package com.senai.cantina.cantina.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/",
                        "/login",
                        "/cadastro",
                        "/cardapio",
                        "/cardapio/**",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/assets/**",
                        "/favicon.ico",
                        "/error"
                ).permitAll()
                .requestMatchers("/gerente/**")
                .hasRole("GERENTE")
                .requestMatchers("/funcionario/**")
                .hasAnyRole(
                        "FUNCIONARIO",
                        "GERENTE"
                )
                .requestMatchers("/pedido/**")
                .hasAnyRole(
                        "CLIENTE",
                        "FUNCIONARIO",
                        "GERENTE"
                )
                .requestMatchers("/pagamento/**")
                .hasAnyRole(
                        "CLIENTE",
                        "FUNCIONARIO",
                        "GERENTE"
                )
                .anyRequest()
                .authenticated()
                )
                .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/inicio", true)
                .failureUrl("/login?erro")
                .permitAll()
                )
                .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
                );

        return http.build();
    }
}
