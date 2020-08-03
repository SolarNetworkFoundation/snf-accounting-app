/* ==================================================================
 * MyBatisConfig.java - 3/08/2020 5:00:59 PM
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

import org.apache.ibatis.session.ExecutorType;
import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.solarnetwork.central.dao.mybatis.type.UUIDTypeHandler;

/**
 * Configuration for MyBatis.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class MyBatisConfig {

  /**
   * Configuration customizer for MyBatis.
   * 
   * @return the customizer
   */
  @Bean
  public ConfigurationCustomizer mybatisConfigurationCustomizer() {
    return new ConfigurationCustomizer() {

      @Override
      public void customize(org.apache.ibatis.session.Configuration configuration) {
        configuration.setDefaultExecutorType(ExecutorType.BATCH);
        configuration.getTypeHandlerRegistry().register(new UUIDTypeHandler());
      }
    };
  }

}
