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
import static net.solarnetwork.javax.money.MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle;
import static org.snf.accounting.cli.ResultPaginationCommands.setNavigationHandler;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.snf.accounting.cli.BaseShellSupport;
import org.snf.accounting.domain.AccountFilter;
import org.snf.accounting.domain.AccountWithBalance;
import org.snf.accounting.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.Aligner;

import com.github.fonimus.ssh.shell.SimpleTable;
import com.github.fonimus.ssh.shell.SimpleTable.SimpleTableBuilder;
import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.dao.FilterResults;

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
   * List accounts.
   * 
   * @param accountId
   *          the account to limit results to
   * @param userId
   *          the SolarNetwork user ID to limit results to
   * @param email
   *          an email substring to limit results to
   * @param max
   *          the maximum number of results, or {@literal 0} for unlimited
   * @param page
   *          the page offset, starting from 1
   */
  @ShellMethod("List accounts.")
  public void accountsList(
      @ShellOption(help = "Email to find (substring match)", defaultValue = "") String email,
      @ShellOption(help = "The account ID to show.", defaultValue = "0") Long accountId,
      @ShellOption(help = "The SolarNetwork user ID to show.", defaultValue = "0") Long userId,
      @ShellOption(help = "The maximum number of results to return, or 0 for unlimited.",
          defaultValue = "0") int max,
      @ShellOption(help = "The result page offset, starting from 1.",
          defaultValue = "1") int page) {
    AccountFilter f = new AccountFilter();
    if (accountId != null && accountId.longValue() > 0) {
      f.setAccountId(accountId);
    }
    if (userId != null && userId.longValue() > 0) {
      f.setUserId(userId);
    }
    if (max >= 1) {
      f.setMax(max);
      if (page > 1) {
        f.setOffset((page - 1) * max);
      }
    }
    if (email != null && !email.isEmpty()) {
      f.setEmail(email);
    }
    doAccountWithBalanceSearch(f);
  }

  private void doAccountWithBalanceSearch(AccountFilter f) {
    setNavigationHandler(f, new Consumer<AccountFilter>() {

      @Override
      public void accept(AccountFilter next) {
        doAccountWithBalanceSearch(next);
      }
    });
    FilterResults<AccountWithBalance, UserLongPK> result = accountService.findFilteredBalances(f);
    // @formatter:off
    SimpleTableBuilder t = SimpleTable.builder()
        .column("ID")
        .column("User ID")
        .column("Info")
        .column("Curr")
        .column("Time Zone")
        .column("Charged")
        .column("Paid")
        .column("Balance")
        ;
    Locale locale = Locale.forLanguageTag("en-NZ");
    for (AccountWithBalance account : result) {
      t.line(asList(
          account.getId().getId(),
          account.getAccount().getUserId(),
          format("%s\n%s", account.getAccount().getAddress().getName(),
              account.getAccount().getAddress().getEmail()),
          account.getAccount().getCurrencyCode(),
          account.getAccount().getAddress().getTimeZoneId(),
          formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
              account.getAccount().getCurrencyCode(), account.getBalance().getChargeTotal()),
          formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
              account.getAccount().getCurrencyCode(), account.getBalance().getPaymentTotal()),
          formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
              account.getAccount().getCurrencyCode(), account.getBalance().getPaymentTotal()
              .subtract(account.getBalance().getChargeTotal()))
          ));
    }
    shell.print(shell.renderTable(buildTable(t.build(), new IntFunction<Iterable<Aligner>>() {

      @Override
      public Iterable<Aligner> apply(int c) {
        return c > 1 && c < 5 ? TOP_LEFT : TOP_RIGHT;
      }
    }, null)));
  }

}
