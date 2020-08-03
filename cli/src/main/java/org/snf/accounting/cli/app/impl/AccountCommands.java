/* ==================================================================
 * AccountCommands.java - 3/08/2020 4:12:42 PM
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

package org.snf.accounting.cli.app.impl;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import org.snf.accounting.cli.BaseShellSupport;
import org.snf.accounting.cli.app.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.table.SimpleHorizontalAligner;

import com.github.fonimus.ssh.shell.SimpleTable;
import com.github.fonimus.ssh.shell.SimpleTable.SimpleTableBuilder;
import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.central.user.billing.snf.domain.Account;

/**
 * Commands for accounts.
 * 
 * @author matt
 * @version 1.0
 */
@SshShellComponent
@ShellCommandGroup("Accounts")
public class AccountCommands extends BaseShellSupport {

  private final AccountService accountService;

  /**
   * Constructor.
   * 
   * @param shell
   *          the shell
   * @param accountService
   *          the account service
   */
  @Autowired
  public AccountCommands(SshShellHelper shell, AccountService accountService) {
    super(shell);
    this.accountService = accountService;
  }

  /**
   * List the available accounts.
   */
  @ShellMethod("Show the available accounts.")
  public void accountsShow() {
    Iterable<Account> accounts = accountService.allAccounts();
    // @formatter:off
    SimpleTableBuilder t = SimpleTable.builder()
        .column("ID")
        .column("User ID")
        .column("Info")
        .column("Curr")
        .column("Time Zone")
        .headerAligner(SimpleHorizontalAligner.left)
        .lineAligner(SimpleHorizontalAligner.left)
        ;
    for (Account account : accounts) {
      t.line(asList(
          account.getId().getId(),
          account.getUserId(),
          format("%s\n%s", account.getAddress().getName(), account.getAddress().getEmail()),
          account.getCurrencyCode(),
          account.getAddress().getTimeZoneId()
          ));
    }
    shell.print(shell.renderTable(t.build()));
    // @formatter:on
  }

}
