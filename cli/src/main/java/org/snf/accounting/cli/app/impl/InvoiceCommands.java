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

import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_DATE;
import static java.util.Arrays.asList;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem.DEFAULT_ITEM_ORDER;
import static net.solarnetwork.javax.money.MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle;

import java.util.Locale;
import java.util.function.Consumer;
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

import net.solarnetwork.central.user.billing.snf.domain.InvoiceImpl;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceFilter;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;
import net.solarnetwork.central.user.billing.snf.domain.UsageInfo;
import net.solarnetwork.central.user.billing.support.LocalizedInvoice;
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
      @ShellOption(help = "The account ID to list invoices for.") Long accountId,
      @ShellOption(help = "The maximum number of results to return, or 0 for unlimited.",
          defaultValue = "0") int max,
      @ShellOption(help = "The result page offset, starting from 1.",
          defaultValue = "1") int page) {
    SnfInvoiceFilter f = SnfInvoiceFilter.forAccount(accountId);
    if (max >= 1) {
      f.setMax(max);
      if (page > 1) {
        f.setOffset((page - 1) * max);
      }
    }
    doInvoiceSearch(f);
  }

  /**
   * Show details for one invoice.
   * 
   * @param invoiceId
   *          the ID of the invoice to show
   */
  @ShellMethod("Show invoice details.")
  public void invoiceShow(@ShellOption(help = "The invoice ID to show.") Long invoiceId) {
    SnfInvoice inv = accountService.invoiceForId(invoiceId);
    if (inv == null) {
      shell.printError(format("Invoice %d not found.", invoiceId));
    }
    Locale locale = actorLocale();
    InvoiceImpl invoice = new InvoiceImpl(inv);
    LocalizedInvoice locInvoice = new LocalizedInvoice(invoice, locale);
    // @formatter:off
    shell.print(format("Invoice %d (INV-%s) - %s - %s (%d) - %s - %s", 
        inv.getId().getId(), 
        invoice.getInvoiceNumber(),
        ISO_DATE.format(inv.getStartDate()),
        inv.getAddress().getName(),
        inv.getAccountId(),
        inv.getCurrencyCode(),
        locInvoice.getLocalizedAmount()));

    // @formatter:off
    SimpleTableBuilder t = SimpleTable.builder()
        .column("Node ID")
        .column("Item ID")
        .column("Key")
        .column("Usage")
        .column("Amount")
        ;
    inv.getItems().stream().sorted(DEFAULT_ITEM_ORDER).forEachOrdered(
        new Consumer<SnfInvoiceItem>() {

        @Override
        public void accept(SnfInvoiceItem itm) {
          UsageInfo usage = itm.getUsageInfo();
          Object nodeId = null;
          if (itm.getMetadata() != null) {
            nodeId = itm.getMetadata().get(SnfInvoiceItem.META_NODE_ID);
          }
          t.line(asList(
              nodeId != null ? nodeId : "",
              itm.getId(),
              itm.getKey(),
              usage != null ? usage.getAmount() : "",
              formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
                  invoice.getCurrencyCode(), itm.getAmount())
              ));
        }
      });
    t.line(asList(
        "",
        "",
        "Subtotal",
        "",
        formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
            invoice.getCurrencyCode(), 
            invoice.getAmount().subtract(invoice.getTaxAmount()))
        ));
    t.line(asList(
        "",
        "",
        "Total",
        "",
        formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
            invoice.getCurrencyCode(), 
            invoice.getAmount())
        ));
    shell.print(shell.renderTable(buildTable(t.build(), new IntFunction<Iterable<Aligner>>() {

      @Override
      public Iterable<Aligner> apply(int c) {
        return c > 0 && c < 3 ? TOP_LEFT : TOP_RIGHT;
      }
    }, null)));
    // @formatter:on
  }

  private void doInvoiceSearch(SnfInvoiceFilter f) {
    FilterResults<SnfInvoice, UserLongPK> result = accountService.findFilteredInvoices(f);
    // @formatter:off
    SimpleTableBuilder t = SimpleTable.builder()
        .column("ID")
        .column("Date")
        .column("Items")
        .column("Amount")
        ;
    Locale locale = actorLocale();
    for (SnfInvoice invoice : result) {
      t.line(asList(
          invoice.getId().getId(),
          ISO_DATE.format(invoice.getStartDate()),
          invoice.getItemCount(),
          formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
              invoice.getCurrencyCode(), invoice.getTotalAmount())
          ));
    }
    shell.print(shell.renderTable(buildTable(t.build(), new IntFunction<Iterable<Aligner>>() {

      @Override
      public Iterable<Aligner> apply(int c) {
        return c == 1 ? TOP_LEFT : TOP_RIGHT;
      }
    }, null)));
  }

}
