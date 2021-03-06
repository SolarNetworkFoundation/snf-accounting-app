/* ==================================================================
 * PaymentDao.java - 5/08/2020 11:28:16 AM
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

import org.snf.accounting.domain.PaymentWithInvoicePayments;

import net.solarnetwork.central.user.billing.snf.domain.Payment;
import net.solarnetwork.central.user.billing.snf.domain.PaymentFilter;
import net.solarnetwork.central.user.domain.UserUuidPK;
import net.solarnetwork.dao.FilterableDao;
import net.solarnetwork.dao.GenericDao;

/**
 * DAO API for {@link Payment} entities.
 * 
 * @author matt
 * @version 1.0
 */
public interface PaymentDao extends GenericDao<PaymentWithInvoicePayments, UserUuidPK>,
    FilterableDao<PaymentWithInvoicePayments, UserUuidPK, PaymentFilter> {

  /**
   * Add a payment.
   * 
   * @param payment
   *          the payment details; the ID will be ignored
   * @param invoiceIds
   *          the optional invoice IDs to associate with the payment
   * @return the resulting payment entity
   */
  PaymentWithInvoicePayments addPayment(Payment payment, Iterable<Long> invoiceIds);

}
