/* ==================================================================
 * MyBatisInvoiceDao.java - 4/08/2020 8:36:04 PM
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
import org.snf.accounting.domain.ExtendedSnfInvoiceFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.user.billing.snf.dao.SnfInvoiceDao;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceFilter;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * MyBatis implementation of {@link SnfInvoiceDao}.
 * 
 * @author matt
 * @version 1.0
 */
@Repository
public class MyBatisInvoiceDao extends BaseMyBatisGenericDaoSupport<SnfInvoice, UserLongPK>
    implements SnfInvoiceDao {

  /** Query name enumeration. */
  public enum QueryName {

    FindFiltered("find-SnfInvoice-for-filter");

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
  public MyBatisInvoiceDao(SqlSessionTemplate template) {
    super(SnfInvoice.class, UserLongPK.class);
    setSqlSessionTemplate(template);
  }

  @Override
  public FilterResults<SnfInvoice, UserLongPK> findFiltered(SnfInvoiceFilter filter,
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

    final ExtendedSnfInvoiceFilter extFilter = ExtendedSnfInvoiceFilter.forFilter(filter).clone();
    if (extFilter.getUnpaidAtDate() != null) {
      extFilter.setUnpaidOnly(true);
    }

    // attempt count first, if max NOT specified as -1 and NOT a mostRecent query
    Long totalCount = null;
    if (max == null || max.intValue() != -1) {
      SnfInvoiceFilter countFilter = extFilter.clone();
      countFilter.setOffset(null);
      countFilter.setMax(null);
      Number n = getSqlSession().selectOne(QueryName.FindFiltered.getCountQueryName(), countFilter);
      if (n != null) {
        totalCount = n.longValue();
      }
    }

    List<SnfInvoice> results = selectList(QueryName.FindFiltered.getQueryName(), extFilter, null,
        null);
    return new BasicFilterResults<>(results, totalCount, offset != null ? offset.intValue() : 0,
        results.size());
  }

}
