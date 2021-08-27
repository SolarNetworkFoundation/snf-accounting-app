/* ==================================================================
 * PaymentWithInvoicePayments.java - 5/08/2020 11:12:33 AM
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

package org.snf.accounting.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.InvoicePayment;
import net.solarnetwork.central.user.billing.snf.domain.Payment;
import net.solarnetwork.central.user.domain.UserUuidPK;

/**
 * Extension of {@link Payment} with a set of associated {@link InvoicePayment}.
 * 
 * @author matt
 * @version 1.0
 */
public class PaymentWithInvoicePayments extends Payment {

  private static final long serialVersionUID = 3023440128831208563L;

  private Account account;
  private Set<InvoicePayment> invoicePayments;

  /**
   * Constructor.
   * 
   * @param accountId
   *          the account ID
   */
  public PaymentWithInvoicePayments(Long accountId) {
    super(accountId);
  }

  /**
   * Constructor.
   * 
   * @param id
   *          the ID
   * @param accountId
   *          the account ID
   * @param created
   *          the creation date
   */
  public PaymentWithInvoicePayments(UserUuidPK id, Long accountId, Instant created) {
    super(id, accountId, created);
  }

  /**
   * Constructor.
   * 
   * @param id
   *          the ID
   * @param userId
   *          the user ID
   * @param accountId
   *          the account ID
   * @param created
   *          the creation date
   */
  public PaymentWithInvoicePayments(UUID id, Long userId, Long accountId, Instant created) {
    super(id, userId, accountId, created);
  }

  /**
   * Get the invoice payments.
   * 
   * @return the invoice payments
   */
  public Set<InvoicePayment> getInvoicePayments() {
    return invoicePayments;
  }

  /**
   * Set the invoice payments.
   * 
   * @param invoicePayments
   *          the invoice payments to set
   */
  public void setInvoicePayments(Set<InvoicePayment> invoicePayments) {
    this.invoicePayments = invoicePayments;
  }

  /**
   * Get the account entity.
   * 
   * @return the account
   */
  public Account getAccount() {
    return account;
  }

  /**
   * Set the account entity.
   * 
   * @param account
   *          the account to set
   */
  public void setAccount(Account account) {
    this.account = account;
  }

}
