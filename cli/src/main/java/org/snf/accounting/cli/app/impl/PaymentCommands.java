/* ==================================================================
 * PaymentCommands.java - 5/08/2020 11:06:42 AM
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

import static java.lang.Long.toUnsignedString;
import static java.lang.String.format;
import static java.time.format.DateTimeFormatter.ISO_DATE;
import static java.util.Arrays.asList;
import static net.solarnetwork.javax.money.MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle;
import static org.snf.accounting.cli.ResultPaginationCommands.setNavigationHandler;

import java.time.ZoneId;
import java.util.Locale;
import java.util.function.Consumer;

import org.snf.accounting.cli.BaseShellSupport;
import org.snf.accounting.cli.CoordinateVisitor;
import org.snf.accounting.domain.PaymentWithInvoicePayments;
import org.snf.accounting.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.Aligner;
import org.springframework.shell.table.KeyValueHorizontalAligner;
import org.springframework.shell.table.KeyValueSizeConstraints;
import org.springframework.shell.table.KeyValueTextWrapper;
import org.springframework.shell.table.TableBuilder.CellMatcherStub;

import com.github.fonimus.ssh.shell.SimpleTable;
import com.github.fonimus.ssh.shell.SimpleTable.SimpleTableBuilder;
import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.central.user.billing.snf.domain.InvoicePayment;
import net.solarnetwork.central.user.billing.snf.domain.PaymentFilter;
import net.solarnetwork.central.user.domain.UserUuidPK;
import net.solarnetwork.dao.FilterResults;

/**
 * Commands for payments.
 * 
 * @author matt
 * @version 1.0
 */
@SshShellComponent
@ShellCommandGroup("Payments")
public class PaymentCommands extends BaseShellSupport {

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
  public PaymentCommands(SshShellHelper shell, AccountService accountService) {
    super(shell);
    this.accountService = accountService;
  }

  /**
   * List payments for account.
   * 
   * @param accountId
   *          the account ID to show payments for
   */
  @ShellMethod("List payments for account.")
  public void paymentsForAccount(
      @ShellOption(help = "The account ID to list invoices for.") Long accountId,
      @ShellOption(help = "The maximum number of results to return, or 0 for unlimited.",
          defaultValue = "0") int max,
      @ShellOption(help = "The result page offset, starting from 1.",
          defaultValue = "1") int page) {
    PaymentFilter f = PaymentFilter.forAccount(accountId);
    if (max >= 1) {
      f.setMax(max);
      if (page > 1) {
        f.setOffset((page - 1) * max);
      }
    }
    doPaymentSearch(f);
  }

  private void doPaymentSearch(PaymentFilter f) {
    setNavigationHandler(f, new Consumer<PaymentFilter>() {

      @Override
      public void accept(PaymentFilter next) {
        doPaymentSearch(next);
      }
    });
    FilterResults<PaymentWithInvoicePayments, UserUuidPK> result = accountService
        .findFilteredPayments(f);
    // @formatter:off
    SimpleTableBuilder t = SimpleTable.builder()
        .column("ID")
        .column("Date")
        .column("Type")
        .column("Amount")
        .column("Invoice")
        ;
    Locale locale = actorLocale();
    for (PaymentWithInvoicePayments pay : result) {
      ZoneId tz = pay.getAccount().getTimeZone();
      StringBuilder buf = new StringBuilder();
      if (pay.getInvoicePayments() != null) {
        pay.getInvoicePayments().stream().sorted(InvoicePayment.SORT_BY_DATE).forEachOrdered(
            new Consumer<InvoicePayment>() {

              @Override
              public void accept(InvoicePayment ip) {
                buf.append(format("INV-%s", toUnsignedString(ip.getInvoiceId(), 36).toUpperCase()))
                  .append(" ")
                  .append(ip.getInvoiceId())
                  .append(": ")
                  .append(formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
                      pay.getCurrencyCode(), ip.getAmount()))
                  .append("\n");
            
              }
            });
      }
      t.line(asList(
          pay.getId().getId(),
          ISO_DATE.format(pay.getCreated().atZone(tz).toLocalDate()),
          pay.getPaymentType().toString(),
          formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
              pay.getCurrencyCode(), pay.getAmount()),
          buf.toString()
          ));
    }
    KeyValueSizeConstraints invPayConstraints = new KeyValueSizeConstraints(":");
    KeyValueHorizontalAligner invPayAligner = new KeyValueHorizontalAligner(":");
    KeyValueTextWrapper invPayWrapper = new KeyValueTextWrapper(":");
    shell.print(renderTable(buildTable(t.build(), new CoordinateVisitor<CellMatcherStub>() {
      
      @Override
      public void visit(int c, int r, CellMatcherStub cell) {
        if (c < 4) {
          for (Aligner a : (c < 2 ? TOP_LEFT : TOP_RIGHT)) {
            cell.addAligner(a);
          }
        } else if (r > 0) {
          cell.addAligner(invPayAligner);
          cell.addSizer(invPayConstraints);
          cell.addWrapper(invPayWrapper);
        }
      }
    })));
  }

}
