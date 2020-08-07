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
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Arrays.asList;
import static net.solarnetwork.javax.money.MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle;
import static org.snf.accounting.cli.ResultPaginationCommands.setNavigationHandler;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

import org.snf.accounting.cli.BaseShellSupport;
import org.snf.accounting.cli.CoordinateVisitor;
import org.snf.accounting.domain.ExtendedSnfInvoiceFilter;
import org.snf.accounting.domain.PaymentWithInvoicePayments;
import org.snf.accounting.domain.SnfInvoiceWithBalance;
import org.snf.accounting.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
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

import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.InvoicePayment;
import net.solarnetwork.central.user.billing.snf.domain.PaymentFilter;
import net.solarnetwork.central.user.billing.snf.util.SnfBillingUtils;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.central.user.domain.UserUuidPK;
import net.solarnetwork.dao.BasicFilterResults;
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
    renderPaymentWithInvoicePaymentsTable(shell, result);
  }

  /**
   * Render a table of payment with invoice payment details.
   * 
   * @param shell
   *          the shell
   * @param result
   *          the results to render
   */
  public static void renderPaymentWithInvoicePaymentsTable(SshShellHelper shell,
      FilterResults<PaymentWithInvoicePayments, UserUuidPK> result) {
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
    // @formatter:on
    KeyValueSizeConstraints invPayConstraints = new KeyValueSizeConstraints(":");
    KeyValueHorizontalAligner invPayAligner = new KeyValueHorizontalAligner(":");
    KeyValueTextWrapper invPayWrapper = new KeyValueTextWrapper(":");
    shell.print(shell.renderTable(
        BaseShellSupport.buildTable(shell, t.build(), new CoordinateVisitor<CellMatcherStub>() {

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

  /**
   * Add payment.
   * 
   * @param accountId
   *          the account ID
   * @param amount
   *          the payment amount
   * @param invoices
   *          a comma-delimited list of invoice IDs or invoice nums
   */
  @ShellMethod("Add payment.")
  @ShellMethodAvailability("adminAvailability")
  public void paymentAdd(
      // CHECKSTYLE OFF: LineLength
      @ShellOption(help = "The account ID to add payment to.") Long accountId,
      @ShellOption(help = "The payment amount.") BigDecimal amount,
      @ShellOption(help = "The invoice IDs to associate the payment with.") String invoices,
      @ShellOption(help = "The payment date, or omit for the current date (YYYY-MM-DD).",
          defaultValue = "") String paymentDate,
      @ShellOption(help = "A reference to save with the payment.", defaultValue = "") String ref,
      @ShellOption(help = "An external key to save with the payment.",
          defaultValue = "") String externalKey) {
    // CHECKSTYLE ON: LineLength
    Set<Long> invoiceIds = new TreeSet<>();
    String[] invoiceComponents = invoices.split(",");
    for (int i = 0; i < invoiceComponents.length; i++) {
      Long id = SnfBillingUtils.invoiceIdForNum(invoiceComponents[i]);
      if (id == null) {
        try {
          id = Long.valueOf(invoiceComponents[i]);
        } catch (NumberFormatException e) {
          // CHECKSTYLE OFF: LineLength
          String err = format(
              "Invalid invoice reference [%s].\nInvoices must be specified as either an integer ID or number like INV-XYZ.",
              invoiceComponents[i]);
          // CHECKSTYLE ON: LineLength
          shell.printError(err);
          return;
        }
      }
      invoiceIds.add(id);
    }

    // get specified invoices to prompt for confirmation
    ExtendedSnfInvoiceFilter invoiceFilter = new ExtendedSnfInvoiceFilter();
    invoiceFilter.setAccountId(accountId);
    invoiceFilter.setInvoiceIds(invoiceIds.toArray(new Long[invoiceIds.size()]));
    FilterResults<SnfInvoiceWithBalance, UserLongPK> invoiceResults = accountService
        .findFilteredInvoices(invoiceFilter);
    if (invoiceResults.getReturnedResultCount() < 1) {
      shell.printError(i18n("payment.noInvoices", "No invoices."));
      return;
    }
    InvoiceCommands.renderInvoiceTable(shell, invoiceResults);

    BigDecimal total = StreamSupport.stream(invoiceResults.spliterator(), false)
        .map(SnfInvoiceWithBalance::getDueAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.compareTo(amount) < 0) {
      if (!promptConfirmOk("payment.largerThanInvoiceAmount",
          "Invoices total {0} but payment is greater by {1}.", total.toPlainString(),
          amount.subtract(total).toPlainString())) {
        return;
      }
      shell.printWarning(i18n("answer.goingAheadAnyway", "If you say so."));
    } else if (total.compareTo(amount) > 0) {
      if (!promptConfirmOk("payment.lessThanInvoiceAmount",
          "Invoices total {0} but payment is less by {1}.", total.toPlainString(),
          total.subtract(amount).toPlainString())) {
        return;
      }
      shell.printWarning(i18n("answer.goingAheadAnyway", "If you say so."));
    }

    Account account;
    try {
      account = accountService.getAccount(accountId);
    } catch (DataAccessException e) {
      shell.printError(i18n("answer.error.accountNotFound", "Account {0} not found.", accountId));
      return;
    }

    Instant ts;
    if (paymentDate != null && !paymentDate.isEmpty()) {
      try {
        ts = LocalDate.parse(paymentDate, ISO_LOCAL_DATE).atStartOfDay().plusHours(12)
            .atZone(account.getTimeZone()).toInstant();
      } catch (DateTimeParseException e) {
        shell.printError(
            i18n("Invalid payment date. Please specify as YYYY-MM-DD format.", "Bad date."));
        return;
      }
    } else {
      ts = Instant.now();
    }

    PaymentWithInvoicePayments result = accountService.addPayment(accountId, invoiceIds, amount, ts,
        ref, externalKey);

    shell.printSuccess(i18n("payment.created", "Payment created.", result.getId().getId()));

    BasicFilterResults<PaymentWithInvoicePayments, UserUuidPK> list = new BasicFilterResults<>(
        Collections.singleton(result));
    renderPaymentWithInvoicePaymentsTable(shell, list);
  }
}
