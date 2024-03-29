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
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.util.Arrays.asList;
import static net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem.DEFAULT_ITEM_ORDER;
import static net.solarnetwork.javax.money.MoneyUtils.formattedMoneyAmountFormatWithSymbolCurrencyStyle;
import static org.snf.accounting.cli.ResultPaginationCommands.setNavigationHandler;
import static org.snf.accounting.cli.ShellUtils.ISO_MONTH;
import static org.snf.accounting.cli.ShellUtils.getBold;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.IntFunction;

import org.snf.accounting.cli.BaseShellSupport;
import org.snf.accounting.domain.ExtendedSnfInvoiceFilter;
import org.snf.accounting.domain.SnfInvoiceWithBalance;
import org.snf.accounting.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.springframework.shell.table.Aligner;

import com.github.fonimus.ssh.shell.PromptColor;
import com.github.fonimus.ssh.shell.SimpleTable;
import com.github.fonimus.ssh.shell.SimpleTable.SimpleTableBuilder;
import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao.InvoiceSortKey;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceImpl;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceFilter;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceItem;
import net.solarnetwork.central.user.billing.snf.domain.UsageInfo;
import net.solarnetwork.central.user.billing.support.LocalizedInvoice;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.GenericDao.StandardSortKey;
import net.solarnetwork.domain.SimpleSortDescriptor;
import net.solarnetwork.domain.SortDescriptor;
import net.solarnetwork.util.StringUtils;

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
   * List invoices across accounts.
   * 
   * @param accountId
   *          the account to limit results to
   * @param minMonth
   *          a minimum invoice date (inclusive)
   * @param maxMonth
   *          a maximum invoice date (exclusive)
   * @param unpaidAt
   *          only show invoices that are unpaid at a given date
   * @param unpaidOnly
   *          only show invoices that are currently unpaid
   * @param max
   *          the maximum number of results, or {@literal 0} for unlimited
   * @param page
   *          the page offset, starting from 1
   */
  @ShellMethod("List invoices.")
  public void invoicesList(
      @ShellOption(help = "The account ID to list invoices for.",
          defaultValue = "0") Long accountId,
      @ShellOption(help = "The minimum invoice month (inclusive) in YYYY-MM.",
          defaultValue = "") String minMonth,
      @ShellOption(help = "The maximum invoice month (exclusive) in YYYY-MM.",
          defaultValue = "") String maxMonth,
      @ShellOption(help = "Only include invoices unpaid at given date (exclusive) in YYYY-MM-DD.",
          defaultValue = "") String unpaidAt,
      @ShellOption(help = "Only include unpaid invoices.", arity = 0) boolean unpaidOnly,
      @ShellOption(
          help = "Sort method: comma-delimited list of ACCOUNT, DATE, and/or ID. "
              + "Any value can have a :d suffix to sort in a descending manner.",
          defaultValue = "") String[] sortBy,
      @ShellOption(help = "The maximum number of results to return, or 0 for unlimited.",
          defaultValue = "0") int max,
      @ShellOption(help = "The result page offset, starting from 1.",
          defaultValue = "1") int page) {
    ExtendedSnfInvoiceFilter f = new ExtendedSnfInvoiceFilter();
    if (accountId != null && accountId.longValue() > 0) {
      f.setAccountId(accountId);
    }
    if (max >= 1) {
      f.setMax(max);
      if (page > 1) {
        f.setOffset((page - 1) * max);
      }
    }
    if (unpaidAt != null && !unpaidAt.isEmpty()) {
      try {
        f.setUnpaidAtDate(
            LocalDate.parse(unpaidAt, ISO_DATE).atStartOfDay(ZoneOffset.UTC).toInstant());
      } catch (DateTimeParseException e) {
        shell.printError("The --unpaid-at value is not valid. Use YYYY-MM-DD syntax.");
        return;
      }
    }
    if (minMonth != null && !minMonth.isEmpty()) {
      try {
        f.setStartDate(YearMonth.parse(minMonth).atDay(1));
      } catch (DateTimeParseException e) {
        shell.printError("The --min-month value is not valid. Use YYYY-MM syntax.");
        return;
      }
    }
    if (maxMonth != null && !maxMonth.isEmpty()) {
      try {
        f.setEndDate(YearMonth.parse(maxMonth).atDay(1));
      } catch (DateTimeParseException e) {
        shell.printError("The --max-month value is not valid. Use YYYY-MM syntax.");
        return;
      }
    }
    f.setUnpaidOnly(unpaidOnly);
    if (sortBy != null && sortBy.length > 0) {
      List<SortDescriptor> sorts = new ArrayList<>(sortBy.length);
      for (String s : sortBy) {
        boolean descending = false;
        if (s.endsWith(":d")) {
          descending = true;
          s = s.substring(0, s.length() - 2);
        }
        try {
          InvoiceSortKey key = InvoiceSortKey.valueOf(s);
          sorts.add(new SimpleSortDescriptor(key.toString(), descending));

        } catch (IllegalArgumentException | NullPointerException e) {
          try {
            StandardSortKey std = StandardSortKey.valueOf(s);
            sorts.add(new SimpleSortDescriptor(std.toString(), descending));
          } catch (IllegalArgumentException | NullPointerException e2) {
            shell.printError("The --short-by value " + s + " is not a valid value. Can be one of:");
            SortedSet<String> keys = new TreeSet<>();
            for (InvoiceSortKey k : InvoiceSortKey.values()) {
              keys.add(k.toString());
            }
            for (StandardSortKey k : StandardSortKey.values()) {
              keys.add(k.toString());
            }
            shell.printError(StringUtils.commaDelimitedStringFromCollection(keys));
            return;
          }
        }
      }
      if (!sorts.isEmpty()) {
        f.setSorts(sorts);
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
    SnfInvoiceWithBalance inv = accountService.invoiceForId(invoiceId);
    if (inv == null) {
      shell.printError(format("Invoice %d not found.", invoiceId));
    }
    renderInvoice(inv);
  }

  private void renderInvoice(SnfInvoiceWithBalance inv) {
    Locale locale = actorLocale();
    NumberFormat numFormat = DecimalFormat.getNumberInstance(locale);
    InvoiceImpl invoice = new InvoiceImpl(inv);
    LocalizedInvoice locInvoice = new LocalizedInvoice(invoice, locale);
    BigDecimal due = inv.getDueAmount();
    // @formatter:off
    shell.print(format("Invoice %d (%s) - %s - %s (%d) - %s - %s - %s", 
        inv.getId().getId(), 
        getBold(invoice.getInvoiceNumber()),
        ISO_DATE.format(inv.getStartDate()),
        inv.getAddress().getName(),
        inv.getAccountId(),
        inv.getCurrencyCode(),
        getBold(locInvoice.getLocalizedAmount()),
        due.compareTo(BigDecimal.ZERO) > 0 
          ? shell.getColored(
              format("DUE: %s", formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
                  invoice.getCurrencyCode(), due)),
              PromptColor.RED)
          : shell.getColored("PAID", PromptColor.GREEN)));

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
              usage != null ? numFormat.format(usage.getAmount()) : "",
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
    if (due.compareTo(BigDecimal.ZERO) > 0) {
      t.line(asList(
          "",
          "",
          "Due",
          "",
          formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
              invoice.getCurrencyCode(), 
              due)
          ));
    }
    shell.print(shell.renderTable(buildTable(t.build(), new IntFunction<Iterable<Aligner>>() {

      @Override
      public Iterable<Aligner> apply(int c) {
        return c > 0 && c < 3 ? TOP_LEFT : TOP_RIGHT;
      }
    }, null)));
    // @formatter:on
  }

  private void doInvoiceSearch(SnfInvoiceFilter f) {
    setNavigationHandler(f, new Consumer<SnfInvoiceFilter>() {

      @Override
      public void accept(SnfInvoiceFilter next) {
        doInvoiceSearch(next);
      }
    });
    FilterResults<SnfInvoiceWithBalance, UserLongPK> result = accountService
        .findFilteredInvoices(f);
    renderInvoiceTable(shell, f, result);
  }

  /**
   * Render an invoice table from invoice results.
   * 
   * @param shell
   *          the shell
   * @param result
   *          the results
   */
  public static void renderInvoiceTable(SshShellHelper shell,
      FilterResults<SnfInvoiceWithBalance, UserLongPK> result) {
    renderInvoiceTable(shell, null, result);
  }

  /**
   * Render an invoice table from invoice results.
   * 
   * @param shell
   *          the shell
   * @filter the filter that produced the results, to use for grouping/subtotals
   * @param result
   *          the results
   */
  public static void renderInvoiceTable(SshShellHelper shell, SnfInvoiceFilter filter,
      FilterResults<SnfInvoiceWithBalance, UserLongPK> result) {
    // @formatter:off
    SimpleTableBuilder t = SimpleTable.builder()
        .column("ID")
        .column("Num")
        .column("Date")
        .column("Acct")
        .column("Items")
        .column("Amount")
        .column("Due")
        ;
    // @formatter:on
    Locale locale = actorLocale();
    String currCode = "NZD";
    boolean accountSubtotals = false;
    Long lastAccountId = null;
    if (filter != null && filter.getSorts() != null && !filter.getSorts().isEmpty()
        && InvoiceSortKey.ACCOUNT.toString().equals(filter.getSorts().get(0).getSortKey())) {
      accountSubtotals = true;
    }
    SortedMap<String, BigDecimal> subtotals = new TreeMap<>();
    SortedMap<String, BigDecimal> subdues = new TreeMap<>();
    SortedMap<String, BigDecimal> totals = new TreeMap<>();
    SortedMap<String, BigDecimal> dues = new TreeMap<>();
    for (SnfInvoiceWithBalance inv : result) {
      currCode = inv.getCurrencyCode();
      totals.compute(currCode, (k, v) -> {
        if (v == null) {
          return inv.getTotalAmount();
        }
        return v.add(inv.getTotalAmount());
      });
      dues.compute(currCode, (k, v) -> {
        if (v == null) {
          return inv.getDueAmount();
        }
        return v.add(inv.getDueAmount());
      });
      if (accountSubtotals && lastAccountId != null && !inv.getAccountId().equals(lastAccountId)) {
        for (String cc : subtotals.keySet()) {
          // @formatter:off
          t.line(asList(
              "",
              "",
              "",
              "Acct " + lastAccountId + " subotal " + cc,
              "",
              formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
                cc, 
                subtotals.get(cc)),
              formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
                  cc, 
                  subdues.get(cc))
              ));
          // @formatter:on
        }
        subtotals.clear();
        subdues.clear();
      }
      lastAccountId = inv.getAccountId();
      subtotals.compute(currCode, (k, v) -> {
        if (v == null) {
          return inv.getTotalAmount();
        }
        return v.add(inv.getTotalAmount());
      });
      subdues.compute(currCode, (k, v) -> {
        if (v == null) {
          return inv.getDueAmount();
        }
        return v.add(inv.getDueAmount());
      });
      InvoiceImpl invoice = new InvoiceImpl(inv);
      // @formatter:off
      t.line(asList(
          inv.getId().getId(), 
          invoice.getInvoiceNumber(),
          format("%s (%s)",
              ISO_DATE.format(inv.getCreated().atZone(inv.getTimeZone()).toLocalDate()),
              ISO_MONTH.format(inv.getStartDate())),
          format("%s (%d)", inv.getAddress().getEmail(), inv.getAccountId()),
          inv.getItemCount(),
          formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, inv.getCurrencyCode(),
              inv.getTotalAmount()),
          formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, inv.getCurrencyCode(),
              inv.getTotalAmount().subtract(inv.getPaidAmount()))));
      // @formatter:on
    }
    if (!totals.isEmpty()) {
      for (String cc : totals.keySet()) {
        // @formatter:off
        t.line(asList(
            "",
            "",
            "",
            "",
            "Total " + cc,
            formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
              cc, 
              totals.get(cc)),
            formattedMoneyAmountFormatWithSymbolCurrencyStyle(locale, 
                cc, 
                dues.get(cc))
            ));
        // @formatter:on
      }
    }

    shell.print(shell.renderTable(
        BaseShellSupport.buildTable(shell, t.build(), new IntFunction<Iterable<Aligner>>() {

          @Override
          public Iterable<Aligner> apply(int c) {
            return c == 2 || c == 3 ? TOP_LEFT : TOP_RIGHT;
          }
        }, null)));
  }

  /**
   * Generate an invoice for an account.
   * 
   * @param accountId
   *          the account ID
   * @param month
   *          the month, in YYYY-MM format
   */
  @ShellMethod("Generate invoice for an account.")
  @org.springframework.shell.standard.ShellMethodAvailability("adminAvailability")
  public void generateInvoiceForAccount(
      @ShellOption(help = "The account ID to generate for.") Long accountId,
      @ShellOption(help = "The month to generate, in YYYY-MM form.") String month) {
    YearMonth ym = YearMonth.parse(month);
    try {
      AccountTask task = accountService.createInvoiceGenerationTask(accountId, ym);
      if (task != null) {
        shell.printSuccess(format("Created invoice generation task %s", task.getId()));
      }
    } catch (DataAccessException e) {
      shell.printError(e.getMessage());
    }
  }

  /**
   * Deliver an invoice for an account.
   * 
   * @param invoiceId
   *          the invoice ID
   */
  @ShellMethod("Deliver invoice for an account.")
  @org.springframework.shell.standard.ShellMethodAvailability("adminAvailability")
  public void deliverInvoice(@ShellOption(help = "The invoice ID to deliver.") Long invoiceId) {
    try {
      AccountTask task = accountService.createInvoiceDeliverTask(invoiceId);
      if (task != null) {
        shell.printSuccess(format("Created invoice deliver task %s", task.getId()));
      }
    } catch (DataAccessException e) {
      shell.printError(e.getMessage());
    }
  }

  /**
   * Create a new credit invoice.
   * 
   * @param accountId
   *          the account ID
   * @param amount
   *          the credit amount
   * @param creditDate
   *          the date for the credit, or {@literal null} for the current date
   * @param description
   *          an optional description
   */
  @ShellMethod("Create a new credit invoice for an account.")
  @org.springframework.shell.standard.ShellMethodAvailability("adminAvailability")
  public void creditAdd(@ShellOption(help = "The account ID to add credit to.") Long accountId,
      @ShellOption(help = "The credit amount.") BigDecimal amount,
      @ShellOption(help = "The credit date, or omit for the current date (YYYY-MM-DD).",
          defaultValue = "") String creditDate,
      @ShellOption(help = "A message to save with the payment.",
          defaultValue = "") String description) {
    Account account;
    try {
      account = accountService.getAccount(accountId);
    } catch (DataAccessException e) {
      shell.printError(i18n("answer.error.accountNotFound", "Account {0} not found.", accountId));
      return;
    }

    Instant ts;
    if (creditDate != null && !creditDate.isEmpty()) {
      try {
        ts = LocalDate.parse(creditDate, ISO_LOCAL_DATE).atStartOfDay().plusHours(12)
            .atZone(account.getTimeZone()).toInstant();
      } catch (DateTimeParseException e) {
        shell.printError(
            i18n("Invalid payment date. Please specify as YYYY-MM-DD format.", "Bad date."));
        return;
      }
    } else {
      ts = Instant.now();
    }

    try {
      SnfInvoiceWithBalance inv = accountService.addCredit(accountId, amount, ts, description);
      if (inv != null) {
        shell.printSuccess(format("Created credit invoice %d", inv.getId().getId()));
        renderInvoice(inv);
      }
    } catch (DataAccessException e) {
      shell.printError(e.getMessage());
    }
  }

}
