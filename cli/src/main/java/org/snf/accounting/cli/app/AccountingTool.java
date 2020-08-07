/* ==================================================================
 * AccountingTool.java - 3/08/2020 11:47:48 AM
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

package org.snf.accounting.cli.app;

import java.util.concurrent.CountDownLatch;

import org.snf.accounting.cli.CliServices;
import org.snf.accounting.cli.app.config.AppConfiguration;
import org.snf.accounting.cli.app.impl.AppServices;
import org.snf.accounting.dao.mybatis.MyBatisDaos;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for SNF Accounting CLI application.
 * 
 * @author matt
 * @version 1.0
 */
@SpringBootApplication(scanBasePackageClasses = { AppConfiguration.class, AppServices.class,
    CliServices.class, MyBatisDaos.class })
@EnableAsync
@EnableScheduling
public class AccountingTool {

  /**
   * Command-line entry point.
   * 
   * @param args
   *          the command-line arguments
   */
  public static void main(String[] args) throws InterruptedException {
    ApplicationContext ctx = new SpringApplicationBuilder().sources(AccountingTool.class)
        .web(WebApplicationType.NONE).logStartupInfo(false).build().run(args);

    // keep the app running as a service
    final CountDownLatch closeLatch = ctx.getBean("closeLatch", CountDownLatch.class);
    Runtime.getRuntime().addShutdownHook(new Thread() {

      @Override
      public void run() {
        closeLatch.countDown();
      }
    });
    closeLatch.await();
  }

  @Bean
  public CountDownLatch closeLatch() {
    return new CountDownLatch(1);
  }

}
