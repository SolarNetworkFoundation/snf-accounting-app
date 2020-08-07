/* ==================================================================
 * AccountFilter.java - 4/08/2020 2:46:05 PM
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

import net.solarnetwork.domain.SimplePagination;

/**
 * Filter for accounts.
 * 
 * @author matt
 * @version 1.0
 */
public class AccountFilter extends SimplePagination {

  private Long userId;
  private Long accountId;
  private String email;

  @Override
  public AccountFilter clone() {
    return (AccountFilter) super.clone();
  }

  /**
   * Get the user ID.
   * 
   * @return the userId
   */
  public Long getUserId() {
    return userId;
  }

  /**
   * Set the user ID.
   * 
   * @param userId
   *          the userId to set
   */
  public void setUserId(Long userId) {
    this.userId = userId;
  }

  /**
   * Get the account ID.
   * 
   * @return the accountId
   */
  public Long getAccountId() {
    return accountId;
  }

  /**
   * Set the account ID.
   * 
   * @param accountId
   *          the accountId to set
   */
  public void setAccountId(Long accountId) {
    this.accountId = accountId;
  }

  /**
   * Get the email.
   * 
   * @return the email
   */
  public String getEmail() {
    return email;
  }

  /**
   * Set the email.
   * 
   * @param email
   *          the email to set
   */
  public void setEmail(String email) {
    this.email = email;
  }

}
