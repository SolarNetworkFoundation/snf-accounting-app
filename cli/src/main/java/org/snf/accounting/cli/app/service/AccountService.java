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

package org.snf.accounting.cli.app.service;

import org.snf.accounting.domain.AccountFilter;
import org.snf.accounting.domain.AccountWithBalance;

import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceFilter;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.dao.FilterResults;

/**
 * Service for dealing with accounts.
 * 
 * @author matt
 * @version 1.0
 */
public interface AccountService {

  /**
   * Get all available accounts.
   * 
   * @return the accounts, never {@literal null}
   */
  Iterable<Account> allAccounts();

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
  FilterResults<SnfInvoice, UserLongPK> findFilteredInvoices(SnfInvoiceFilter filter);

}
