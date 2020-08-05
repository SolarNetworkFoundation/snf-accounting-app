/* ==================================================================
 * MyBatisAccountDao.java - 3/08/2020 4:29:21 PM
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

package org.snf.accounting.dao.mybatis;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.snf.accounting.dao.AccountDao;
import org.snf.accounting.domain.AccountFilter;
import org.snf.accounting.domain.AccountWithBalance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountTask;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * JDBC implementation of {@code AccountDao}.
 * 
 * @author matt
 * @version 1.0
 */
@Repository
public class MyBatisAccountDao extends BaseMyBatisGenericDaoSupport<Account, UserLongPK>
    implements AccountDao {

  /** Query name enumeration. */
  public enum QueryName {

    FindFilteredBalance("find-AccountWithBalance-for-filter"),

    SaveTask("insert-AccountTask");

    private final String queryName;

    private QueryName(String queryName) {
      this.queryName = queryName;
    }

    /**
     * Get the query name.
     * 
     * @return the query name
     */
    public String getQueryName() {
      return queryName;
    }

    /**
     * Get the query name to use for a count-only result.
     * 
     * @return the count query name
     */
    public String getCountQueryName() {
      return queryName + "-count";
    }

  }

  /**
   * Constructor.
   * 
   * @param template
   *          the template
   */
  @Autowired
  public MyBatisAccountDao(SqlSessionTemplate template) {
    super(Account.class, UserLongPK.class);
    setSqlSessionTemplate(template);
  }

  @Override
  public FilterResults<AccountWithBalance, UserLongPK> findFilteredBalances(AccountFilter filter,
      List<SortDescriptor> sorts, Integer offset, Integer max) {
    if (offset != null || max != null || sorts != null) {
      filter = filter.clone();
      filter.setSorts(sorts);
      filter.setMax(max);
      if (offset == null) {
        // force offset to 0 if implied
        filter.setOffset(0);
      } else {
        filter.setOffset(offset);
      }
    }

    // attempt count first, if max NOT specified as -1 and NOT a mostRecent query
    Long totalCount = null;
    if (max == null || max.intValue() != -1) {
      AccountFilter countFilter = filter.clone();
      countFilter.setOffset(null);
      countFilter.setMax(null);
      Number n = getSqlSession().selectOne(QueryName.FindFilteredBalance.getCountQueryName(),
          countFilter);
      if (n != null) {
        totalCount = n.longValue();
      }
    }

    List<AccountWithBalance> results = selectList(QueryName.FindFilteredBalance.getQueryName(),
        filter, null, null);
    return new BasicFilterResults<>(results, totalCount, offset != null ? offset.intValue() : 0,
        results.size());
  }

  @Override
  public void saveTask(AccountTask task) {
    getSqlSessionTemplate().insert(QueryName.SaveTask.getQueryName(), task);
  }

}
