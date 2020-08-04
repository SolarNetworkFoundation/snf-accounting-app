/* ==================================================================
 * InvoiceCommands.java - 4/08/2020 10:18:45 PM
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

import static java.util.Arrays.asList;
import static net.solarnetwork.javax.money.MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle;

import java.util.Locale;
import java.util.function.IntFunction;

import org.snf.accounting.cli.BaseShellSupport;
import org.snf.accounting.cli.app.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.Aligner;

import com.github.fonimus.ssh.shell.SimpleTable;
import com.github.fonimus.ssh.shell.SimpleTable.SimpleTableBuilder;
import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceFilter;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.dao.FilterResults;

/**
 * Commands for accounts.
 * 
 * @author matt
 * @version 1.0
 */
@SshShellComponent
@ShellCommandGroup("Invoices")
public class InvoiceCommands extends BaseShellSupport {

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
  public InvoiceCommands(SshShellHelper shell, AccountService accountService) {
    super(shell);
    this.accountService = accountService;
  }

  /**
   * List invoices for account.
   * 
   * @param accountId
   *          the account ID to show invoices for
   */
  @ShellMethod("List invoices for account.")
  public void invoicesForAccount(
      @ShellOption(help = "The account ID to list invoices for.") Long accountId) {
    SnfInvoiceFilter f = SnfInvoiceFilter.forAccount(accountId);
    doInvoiceSearch(f);
  }

  private void doInvoiceSearch(SnfInvoiceFilter f) {
    FilterResults<SnfInvoice, UserLongPK> result = accountService.findFilteredInvoices(f);
    // @formatter:off
    SimpleTableBuilder t = SimpleTable.builder()
        .column("ID")
        .column("Amount")
        ;
    Locale locale = Locale.forLanguageTag("en-NZ");
    for (SnfInvoice invoice : result) {
      t.line(asList(
          invoice.getId().getId(),
          formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
              invoice.getCurrencyCode(), invoice.getTotalAmount())
          ));
    }
    shell.print(shell.renderTable(buildTable(t.build(), new IntFunction<Iterable<Aligner>>() {

      @Override
      public Iterable<Aligner> apply(int c) {
        return TOP_RIGHT;
      }
    }, null)));
  }

}
