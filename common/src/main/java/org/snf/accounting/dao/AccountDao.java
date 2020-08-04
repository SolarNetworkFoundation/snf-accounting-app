/* ==================================================================
 * AccountDao.java - 3/08/2020 4:25:52 PM
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

package org.snf.accounting.dao;

import java.util.List;

import org.snf.accounting.domain.AccountFilter;
import org.snf.accounting.domain.AccountWithBalance;

import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.dao.GenericDao;
import net.solarnetwork.domain.SortDescriptor;

/**
 * DAO API for accounts.
 * 
 * @author matt
 * @version 1.0
 */
public interface AccountDao extends GenericDao<Account, UserLongPK> {

  /**
   * API for querying for a filtered set of account balances from all possible results.
   * 
   * @param filter
   *          the query filter
   * @param sorts
   *          the optional sort descriptors
   * @param offset
   *          an optional result offset
   * @param max
   *          an optional maximum number of returned results
   * @return the results, never {@literal null}
   */
  FilterResults<AccountWithBalance, UserLongPK> findFilteredBalances(AccountFilter filter,
      List<SortDescriptor> sorts, Integer offset, Integer max);

}
