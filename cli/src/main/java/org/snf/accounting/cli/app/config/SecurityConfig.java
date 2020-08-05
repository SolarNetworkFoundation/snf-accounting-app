/* ==================================================================
 * SecurityConfig.java - 3/08/2020 3:08:55 PM
 * 
 * Copyright 2020 SolarNetwork Foundation
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 * 02111-1307 USA
 * ==================================================================
 */

package org.snf.accounting.cli.app.config;

import org.snf.accounting.impl.SimpleUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  /**
   * A prefix to use for {@code passwordFile} to use a class path resource.
   */
  public static final String CLASSPATH_FILE_PREFIX = "classpath:";

  @Value("${app.auth.password-file:classpath:default-passwords.txt}")
  private String passwordFile = "classpath:default-passwords.txt";

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * The user details service.
   * 
   * @return the service
   */
  @Bean
  public UserDetailsService userDetailsService() {
    Resource passwordResource;
    if (passwordFile.toLowerCase().startsWith(CLASSPATH_FILE_PREFIX)) {
      passwordResource = new ClassPathResource(
          passwordFile.substring(CLASSPATH_FILE_PREFIX.length()));
    } else {
      passwordResource = new FileSystemResource(passwordFile);
    }
    return new SimpleUserDetailsService(passwordResource);
  }

  /**
   * The authentication provider service.
   * 
   * @return the service
   */
  @Bean("snfAuthProvider")
  public AuthenticationManager authenticationManager() {
    DaoAuthenticationProvider p = new DaoAuthenticationProvider();
    p.setUserDetailsService(userDetailsService());
    p.setPasswordEncoder(passwordEncoder());
    return new ProviderManager(p);
  }

}
