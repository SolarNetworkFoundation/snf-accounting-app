/* ==================================================================
 * ExtendedSnfInvoiceFilter.java - 6/08/2020 9:38:49 AM
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

import net.solarnetwork.central.user.billing.snf.domain.SnfInvoiceFilter;

/**
 * Extension of {@link SnfInvoiceFilter}.
 * 
 * @author matt
 * @version 1.0
 */
public class ExtendedSnfInvoiceFilter extends SnfInvoiceFilter {

  /**
   * Get an extended filter instance based on a filter instance.
   * 
   * @param f
   *          the filter
   * @return the extended filter; if {@code f} is already one it will be returned directly
   */
  public static ExtendedSnfInvoiceFilter forFilter(SnfInvoiceFilter f) {
    if (f instanceof ExtendedSnfInvoiceFilter) {
      return (ExtendedSnfInvoiceFilter) f;
    }
    ExtendedSnfInvoiceFilter copy = new ExtendedSnfInvoiceFilter();
    copy.setAccountId(f.getAccountId());
    copy.setEndDate(f.getEndDate());
    copy.setMax(f.getMax());
    copy.setOffset(f.getOffset());
    copy.setSorts(f.getSorts());
    copy.setStartDate(f.getStartDate());
    copy.setUnpaidOnly(f.getUnpaidOnly());
    copy.setUserId(f.getUserId());
    return copy;
  }

  private Long[] invoiceIds;
  private Instant unpaidAtDate;

  @Override
  public ExtendedSnfInvoiceFilter clone() {
    return (ExtendedSnfInvoiceFilter) super.clone();
  }

  /**
   * Get the invoice IDs.
   * 
   * @return the invoice IDs
   */
  public Long[] getInvoiceIds() {
    return invoiceIds;
  }

  /**
   * Set the invoice IDs.
   * 
   * @param invoiceIds
   *          the invoiceIds to set
   */
  public void setInvoiceIds(Long[] invoiceIds) {
    this.invoiceIds = invoiceIds;
  }

  /**
   * Get the unpaid at date.
   * 
   * @return the unpaid at date
   */
  public Instant getUnpaidAtDate() {
    return unpaidAtDate;
  }

  /**
   * Set the unpaid at date.
   * 
   * @param unpaidAtDate
   *          the date
   */
  public void setUnpaidAtDate(Instant unpaidAtDate) {
    this.unpaidAtDate = unpaidAtDate;
  }

}
