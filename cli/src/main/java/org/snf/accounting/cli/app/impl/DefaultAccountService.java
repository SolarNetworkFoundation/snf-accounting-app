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

package org.snf.accounting.cli.app.impl;

import org.snf.accounting.cli.app.service.AccountService;
import org.snf.accounting.dao.AccountDao;
import org.snf.accounting.dao.AddressDao;
import org.snf.accounting.domain.AccountFilter;
import org.snf.accounting.domain.AccountWithBalance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceFilter;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.dao.FilterResults;

/**
 * Default implementation of {@link AccountService}.
 * 
 * @author matt
 * @version 1.0
 */
@Service
public class DefaultAccountService implements AccountService {

  private final AddressDao addressDao;
  private final AccountDao accountDao;
  private final SnfInvoiceDao invoiceDao;

  /**
   * Constructor.
   * 
   * @param addressDao
   *          the address DAO
   * @param accountDao
   *          the account DAO
   * @param invoiceDao
   *          the invoice DAO
   */
  @Autowired
  public DefaultAccountService(AddressDao addressDao, AccountDao accountDao,
      SnfInvoiceDao invoiceDao) {
    super();
    this.addressDao = addressDao;
    this.accountDao = accountDao;
    this.invoiceDao = invoiceDao;
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public Iterable<Account> allAccounts() {
    return accountDao.getAll(null);
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public FilterResults<AccountWithBalance, UserLongPK> findFilteredBalances(AccountFilter filter) {
    return accountDao.findFilteredBalances(filter, filter.getSorts(), filter.getOffset(),
        filter.getMax());
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  @Override
  public FilterResults<SnfInvoice, UserLongPK> findFilteredInvoices(SnfInvoiceFilter filter) {
    return invoiceDao.findFiltered(filter, filter.getSorts(), filter.getOffset(), filter.getMax());
  }

}
