/* ==================================================================
 * MyBatisPaymentDao.java - 5/08/2020 11:29:54 AM
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.mybatis.spring.SqlSessionTemplate;
import org.snf.accounting.dao.PaymentDao;
import org.snf.accounting.domain.ExtendedPaymentFilter;
import org.snf.accounting.domain.PaymentWithInvoicePayments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.user.billing.snf.domain.Payment;
import net.solarnetwork.central.user.billing.snf.domain.PaymentFilter;
import net.solarnetwork.central.user.domain.UserUuidPK;
import net.solarnetwork.dao.BasicFilterResults;
import net.solarnetwork.dao.FilterResults;
import net.solarnetwork.domain.SortDescriptor;

/**
 * MyBatis implementatino of {@link PaymentDao}.
 * 
 * @author matt
 * @version 1.0
 */
@Repository
public class MyBatisPaymentDao extends
    BaseMyBatisGenericDaoSupport<PaymentWithInvoicePayments, UserUuidPK> implements PaymentDao {

  /** Query name enumeration. */
  public enum QueryName {

    FindFiltered("find-Payment-for-filter"),

    AddPayment("add-payment");

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
  public MyBatisPaymentDao(SqlSessionTemplate template) {
    super(PaymentWithInvoicePayments.class, UserUuidPK.class);
    setSqlSessionTemplate(template);
  }

  @Override
  public FilterResults<PaymentWithInvoicePayments, UserUuidPK> findFiltered(PaymentFilter filter,
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

    final ExtendedPaymentFilter extFilter = ExtendedPaymentFilter.forFilter(filter);

    // attempt count first, if max NOT specified as -1 and NOT a mostRecent query
    Long totalCount = null;
    if (max == null || max.intValue() != -1) {
      PaymentFilter countFilter = extFilter.clone();
      countFilter.setOffset(null);
      countFilter.setMax(null);
      Number n = getSqlSession().selectOne(QueryName.FindFiltered.getCountQueryName(), countFilter);
      if (n != null) {
        totalCount = n.longValue();
      }
    }

    List<PaymentWithInvoicePayments> results = selectList(QueryName.FindFiltered.getQueryName(),
        extFilter, null, null);
    return new BasicFilterResults<>(results, totalCount, offset != null ? offset.intValue() : 0,
        results.size());
  }

  @Override
  public PaymentWithInvoicePayments addPayment(Payment payment, Iterable<Long> invoiceIds) {
    Map<String, Object> params = new HashMap<>(2);
    params.put("payment", payment);
    if (invoiceIds != null) {
      Long[] ids = StreamSupport.stream(invoiceIds.spliterator(), false).toArray(Long[]::new);
      if (ids.length > 0) {
        params.put("invoiceIds", ids);
      }
    }
    Payment entity = selectFirst(QueryName.AddPayment.getQueryName(), params);

    ExtendedPaymentFilter filter = new ExtendedPaymentFilter();
    filter.setPaymentIds(new UUID[] { entity.getId().getId() });
    FilterResults<PaymentWithInvoicePayments, UserUuidPK> results = findFiltered(filter, null, null,
        null);
    if (results.getReturnedResultCount() > 0) {
      return results.iterator().next();
    }

    // shouldn't really arrive here
    PaymentWithInvoicePayments result = new PaymentWithInvoicePayments(entity.getId(),
        entity.getAccountId(), entity.getCreated());
    result.setAmount(entity.getAmount());
    result.setCurrencyCode(entity.getCurrencyCode());
    result.setExternalKey(entity.getExternalKey());
    result.setPaymentType(entity.getPaymentType());
    result.setReference(entity.getReference());
    return result;
  }

}
