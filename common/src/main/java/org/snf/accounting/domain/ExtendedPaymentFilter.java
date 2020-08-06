/* ==================================================================
 * ExtendedPaymentFilter.java - 6/08/2020 11:24:07 AM
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

import java.util.UUID;

import net.solarnetwork.central.user.billing.snf.domain.PaymentFilter;

/**
 * Extended version of {@link PaymentFilter}.
 * 
 * @author matt
 * @version 1.0
 */
public class ExtendedPaymentFilter extends PaymentFilter {

  /**
   * Get an extended filter instance based on a filter instance.
   * 
   * @param f
   *          the filter
   * @return the extended filter; if {@code f} is already one it will be returned directly
   */
  public static ExtendedPaymentFilter forFilter(PaymentFilter f) {
    if (f instanceof ExtendedPaymentFilter) {
      return (ExtendedPaymentFilter) f;
    }
    ExtendedPaymentFilter copy = new ExtendedPaymentFilter();
    copy.setAccountId(f.getAccountId());
    copy.setEndDate(f.getEndDate());
    copy.setMax(f.getMax());
    copy.setOffset(f.getOffset());
    copy.setSorts(f.getSorts());
    copy.setStartDate(f.getStartDate());
    copy.setUserId(f.getUserId());
    return copy;
  }

  private UUID[] paymentIds;

  /**
   * Get the payment IDs.
   * 
   * @return the payment IDs
   */
  public UUID[] getPaymentIds() {
    return paymentIds;
  }

  /**
   * Set the payment IDs.
   * 
   * @param paymentIds
   *          the payment IDs to set
   */
  public void setPaymentIds(UUID[] paymentIds) {
    this.paymentIds = paymentIds;
  }

}
