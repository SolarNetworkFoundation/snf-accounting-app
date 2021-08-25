/* ==================================================================
 * SnfInvoiceWithBalance.java - 5/08/2020 10:37:52 AM
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

import java.math.BigDecimal;
import java.time.Instant;

import net.solarnetwork.central.user.billing.snf.domain.SnfInvoice;
import net.solarnetwork.central.user.domain.UserLongPK;

/**
 * Extension of {@link SnfInvoice} with balance information.
 * 
 * @author matt
 * @version 1.0
 */
public class SnfInvoiceWithBalance extends SnfInvoice {

  private static final long serialVersionUID = 2434501509560277928L;

  private BigDecimal paidAmount = BigDecimal.ZERO;

  /**
   * Default constructor.
   * 
   * @param accountId
   *          the account ID
   */
  public SnfInvoiceWithBalance(Long accountId) {
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
  public SnfInvoiceWithBalance(UserLongPK id, Long accountId, Instant created) {
    super(id, accountId, created);
  }

  /**
   * Constructor.
   * 
   * @param accountId
   *          the account ID
   * @param userId
   *          the user ID
   * @param created
   *          the creation date
   */
  public SnfInvoiceWithBalance(Long accountId, Long userId, Instant created) {
    super(accountId, userId, created);
  }

  /**
   * Constructor.
   * 
   * @param id
   *          the UUID ID
   * @param userId
   *          the user ID
   * @param accountId
   *          the account ID
   * @param created
   *          the creation date
   */
  public SnfInvoiceWithBalance(Long id, Long userId, Long accountId, Instant created) {
    super(id, userId, accountId, created);
  }

  /**
   * Get the total amount not yet paid.
   * 
   * @return the due amount
   */
  public BigDecimal getDueAmount() {
    BigDecimal tot = getTotalAmount();
    BigDecimal paid = getPaidAmount();
    return tot.subtract(paid);
  }

  /**
   * Get the paid amount.
   * 
   * @return the paidAmount
   */
  public BigDecimal getPaidAmount() {
    return paidAmount;
  }

  /**
   * Set the paid amount.
   * 
   * @param paidAmount
   *          the paidAmount to set
   */
  public void setPaidAmount(BigDecimal paidAmount) {
    if (paidAmount == null) {
      paidAmount = BigDecimal.ZERO;
    }
    this.paidAmount = paidAmount;
  }

}
