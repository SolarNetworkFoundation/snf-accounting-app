/* ==================================================================
 * SimpleUserDetailsService.java - 3/08/2020 3:27:05 PM
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

package org.snf.accounting.impl;

import static java.util.stream.Collectors.toCollection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Basic {@link UserDetailsService} that loads read-only password information from a resource.
 * 
 * @author matt
 * @version 1.0
 */
public class SimpleUserDetailsService implements UserDetailsService {

  private final Map<String, UserDetails> users;

  /**
   * Constructor.
   * 
   * @param passwordResource
   *          the password resource to load
   */
  public SimpleUserDetailsService(Resource passwordResource) {
    super();
    this.users = parseResource(passwordResource);
  }

  private static Map<String, UserDetails> parseResource(Resource resource) {
    try (InputStream in = resource.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
      Map<String, UserDetails> users = new LinkedHashMap<>(4);
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.startsWith("#") || line.isEmpty()) {
          continue;
        }
        String[] components = line.split(":");
        if (components.length < 3) {
          continue;
        }
        String username = components[0].toLowerCase();
        String password = components[1];
        String[] roles = components[2].split(",");
        Set<GrantedAuthority> authorities = Arrays.stream(roles)
            .map(e -> new SimpleGrantedAuthority(e)).collect(toCollection(LinkedHashSet::new));
        User user = new User(username, password, authorities);
        users.put(username, user);
      }
      return users;
    } catch (IOException e) {
      String err = String.format("Unable to read password database from resource [%s]: %s",
          resource, e.getMessage());
      throw new RuntimeException(err, e);
    }
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserDetails user = users.get(username.toLowerCase());
    if (user == null) {
      throw new UsernameNotFoundException("User not available.");
    }
    return User.withUserDetails(user).build();
  }

}
