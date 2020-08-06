/* ==================================================================
 * DefaultAccountService.java - 4/08/2020 9:54:29 AM
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

package org.snf.accounting.impl;

import static net.solarnetwork.central.user.billing.snf.domain.AccountTask.newTask;
import static net.solarnetwork.central.user.billing.snf.domain.AccountTaskType.DeliverInvoice;
import static net.solarnetwork.central.user.billing.snf.domain.AccountTaskType.GenerateInvoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.snf.accounting.dao.AccountDao;
import org.snf.accounting.dao.AddressDao;
import org.snf.accounting.dao.PaymentDao;
import org.snf.accounting.domain.AccountFilter;
import org.snf.accounting.domain.AccountWithBalance;
import org.snf.accounting.domain.PaymentWithInvoicePayments;
import org.snf.accounting.domain.SnfInvoiceWithBalance;
import org.snf.accounting.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.billing.snf.domain.InvoiceImpl;
import net.solarnetwork.central.user.billing.snf.domain.Payment;
import net.solarnetwork.central.user.billing.snf.domain.PaymentFilter;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceFilter;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.central.user.domain.UserUuidPK;
import net.solarnetwork.dao.FilterResults;

/**
 * Default implementation of {@link AccountService}.
 * 
 * @author matt
 * @version 1.0
 */
@Service
public class DefaultAccountService implements AccountService {

  @SuppressWarnings("unused")
  private final AddressDao addressDao;
  private final AccountDao accountDao;
  private final SnfInvoiceDao invoiceDao;
  private final PaymentDao paymentDao;

  /**
   * Constructor.
   * 
   * @param addressDao
   *          the address DAO
   * @param accountDao
   *          the account DAO
   * @param invoiceDao
   *          the invoice DAO
   * @param paymentDao
   *          the payment DAO
   */
  @Autowired
  public DefaultAccountService(AddressDao addressDao, AccountDao accountDao,
      SnfInvoiceDao invoiceDao, PaymentDao paymentDao) {
    super();
    this.addressDao = addressDao;
    this.accountDao = accountDao;
    this.invoiceDao = invoiceDao;
    this.paymentDao = paymentDao;
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public Iterable<Account> allAccounts() {
    return accountDao.getAll(null);
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public Account getAccount(Long accountId) {
    Account account = accountDao.get(new UserLongPK(null, accountId));
    if (account == null) {
      String err = String.format("Account %d not found.", accountId);
      throw new EmptyResultDataAccessException(err, 1);
    }
    return account;
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public FilterResults<AccountWithBalance, UserLongPK> findFilteredBalances(AccountFilter filter) {
    return accountDao.findFilteredBalances(filter, filter.getSorts(), filter.getOffset(),
        filter.getMax());
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public FilterResults<SnfInvoiceWithBalance, UserLongPK> findFilteredInvoices(
      SnfInvoiceFilter filter) {
    return (FilterResults) invoiceDao.findFiltered(filter, filter.getSorts(), filter.getOffset(),
        filter.getMax());
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public SnfInvoiceWithBalance invoiceForId(Long invoiceId) {
    UserLongPK id = new UserLongPK(null, invoiceId);
    return (SnfInvoiceWithBalance) invoiceDao.get(id);
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public FilterResults<PaymentWithInvoicePayments, UserUuidPK> findFilteredPayments(
      PaymentFilter filter) {
    return paymentDao.findFiltered(filter, filter.getSorts(), filter.getOffset(), filter.getMax());
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public AccountTask createInvoiceGenerationTask(final Long accountId, final YearMonth month) {
    final LocalDate date = month.atDay(1);
    final Account account = getAccount(accountId);

    SnfInvoiceFilter f = SnfInvoiceFilter.forAccount(account);
    f.setStartDate(date);
    f.setEndDate(date.plusMonths(1));
    FilterResults<SnfInvoiceWithBalance, UserLongPK> existing = findFilteredInvoices(f);
    if (existing.getReturnedResultCount() > 0) {
      SnfInvoiceWithBalance inv = existing.iterator().next();
      InvoiceImpl invoice = new InvoiceImpl(inv);
      String err = String.format("Account %d already has invoice %d (%s) for %s.", accountId,
          inv.getId().getId(), invoice.getInvoiceNumber(), month.toString());
      throw new DuplicateKeyException(err);
    }

    ZonedDateTime targetDate = date.atStartOfDay(account.getTimeZone());
    AccountTask task = newTask(targetDate.toInstant(), GenerateInvoice, accountId);
    accountDao.saveTask(task);
    return task;
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public AccountTask createInvoiceDeliverTask(final Long invoiceId) {
    SnfInvoice invoice = invoiceDao.get(new UserLongPK(null, invoiceId));
    if (invoice == null) {
      String err = String.format("Invoice %d not found.", invoiceId);
      throw new EmptyResultDataAccessException(err, 1);
    }
    Map<String, Object> taskData = new LinkedHashMap<>(2);
    taskData.put("userId", invoice.getUserId());
    taskData.put("id", invoice.getId().getId());
    AccountTask task = newTask(Instant.now(), DeliverInvoice, invoice.getAccountId(), taskData);
    accountDao.saveTask(task);
    return task;
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRED)
  @Override
  public PaymentWithInvoicePayments addPayment(Long accountId, Set<Long> invoiceIds,
      BigDecimal amount, Instant paymentDate, String ref, String externalKey) {
    Payment payment = new Payment(null, accountId, paymentDate);
    payment.setAmount(amount);
    payment.setReference(ref);
    payment.setExternalKey(externalKey);
    return paymentDao.addPayment(payment, invoiceIds);
  }

}
