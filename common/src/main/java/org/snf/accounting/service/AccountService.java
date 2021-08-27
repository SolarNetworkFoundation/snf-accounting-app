/* ==================================================================
 * AccountService.java - 3/08/2020 4:13:38 PM
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

package org.snf.accounting.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Set;

import org.snf.accounting.domain.AccountFilter;
import org.snf.accounting.domain.AccountWithBalance;
import org.snf.accounting.domain.PaymentWithInvoicePayments;
import org.snf.accounting.domain.SnfInvoiceWithBalance;

import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.PaymentFilter;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceFilter;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.central.user.domain.UserUuidPK;
import net.solarnetwork.dao.FilterResults;

/**
 * Service for dealing with accounts.
 * 
 * @author matt
 * @version 1.1
 */
public interface AccountService {

  /**
   * Get all available accounts.
   * 
   * @return the accounts, never {@literal null}
   */
  Iterable<Account> allAccounts();

  /**
   * Get a specific account.
   * 
   * @param accountId
   *          the account ID
   * @return the account
   * @throws org.springframework.dao.EmptyResultDataAccessException
   *           if the account is not found
   */
  Account getAccount(Long accountId);

  /**
   * Find accounts with balance info.
   * 
   * @param filter
   *          the filter
   * @return the results, never {@literal null}
   */
  FilterResults<AccountWithBalance, UserLongPK> findFilteredBalances(AccountFilter filter);

  /**
   * Find invoices.
   * 
   * @param filter
   *          the filter
   * @return the results, never {@literal null}
   */
  FilterResults<SnfInvoiceWithBalance, UserLongPK> findFilteredInvoices(SnfInvoiceFilter filter);

  /**
   * Get a specific invoice.
   * 
   * @param invoiceId
   *          the ID of the invoice to get
   * @return the invoice
   */
  SnfInvoiceWithBalance invoiceForId(Long invoiceId);

  /**
   * Find payments.
   * 
   * @param filter
   *          the filter
   * @return the results, never {@literal null}
   */
  FilterResults<PaymentWithInvoicePayments, UserUuidPK> findFilteredPayments(PaymentFilter filter);

  /**
   * Create an invoice generation task for a given account and month.
   * 
   * @param accountId
   *          the account ID
   * @param month
   *          the month
   * @return the account task
   */
  AccountTask createInvoiceGenerationTask(Long accountId, YearMonth month);

  /**
   * Create an invoice deliver task for a given invoice.
   * 
   * @param invoiceId
   *          the invoice ID
   * @return the account task
   */
  AccountTask createInvoiceDeliverTask(final Long invoiceId);

  /**
   * Add payment to an account, associated with invoices.
   * 
   * @param accountId
   *          the account ID
   * @param invoiceIds
   *          the invoice IDs to associate with the payment
   * @param amount
   *          the payment amount
   * @param paymentDate
   *          the payment date
   * @param ref
   *          the optional payment reference
   * @param externalKey
   *          the optional payment external key
   * @return the payment entity
   */
  PaymentWithInvoicePayments addPayment(Long accountId, Set<Long> invoiceIds, BigDecimal amount,
      Instant paymentDate, String ref, String externalKey);

  /**
   * Add a new credit to an account.
   * 
   * <p>
   * This will generate a new invoice with a single credit item included.
   * </p>
   * 
   * @param accountId
   *          the account ID
   * @param amount
   *          the credit amount
   * @param creditDate
   *          the credit date
   * @param description
   *          an optional message to attach to the credit item
   * @return the newly created invoice
   * @since 1.1
   */
  SnfInvoiceWithBalance addCredit(Long accountId, BigDecimal amount, Instant creditDate,
      String description);

}
