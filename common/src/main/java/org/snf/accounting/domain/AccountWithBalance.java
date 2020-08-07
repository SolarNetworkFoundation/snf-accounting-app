/* ==================================================================
 * AccountWithBalance.java - 4/08/2020 2:36:13 PM
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
import java.util.Objects;

import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.AccountBalance;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.domain.Identity;

/**
 * Combination of account and account balance details.
 * 
 * @author matt
 * @version 1.0
 */
public class AccountWithBalance implements Identity<UserLongPK> {

  private Account account;
  private AccountBalance balance;

  /**
   * Default constructor.
   */
  public AccountWithBalance() {
    super();
  }

  /**
   * Construct with an account.
   * 
   * @param account
   *          the account
   */
  public AccountWithBalance(Account account) {
    super();
    this.account = account;
    this.balance = new AccountBalance(account.getId(), account.getCreated(), BigDecimal.ZERO,
        BigDecimal.ZERO, BigDecimal.ZERO);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("AccountWithBalance{id=");
    builder.append(getId());
    builder.append("}");
    return builder.toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(account);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AccountWithBalance)) {
      return false;
    }
    AccountWithBalance other = (AccountWithBalance) obj;
    return Objects.equals(account, other.account);
  }

  @Override
  public int compareTo(UserLongPK o) {
    UserLongPK id = getId();
    return (id == o ? 0 : id == null ? 1 : id.compareTo(o));
  }

  @Override
  public UserLongPK getId() {
    return (account != null ? account.getId() : null);
  }

  @Override
  public boolean hasId() {
    UserLongPK id = getId();
    return (id != null && id.getId() != null && id.getUserId() != null);
  }

  /**
   * Get the account.
   * 
   * @return the account
   */
  public Account getAccount() {
    return account;
  }

  /**
   * Set the account.
   * 
   * @param account
   *          the account to set
   */
  public void setAccount(Account account) {
    this.account = account;
  }

  /**
   * Get the balance.
   * 
   * @return the balance
   */
  public AccountBalance getBalance() {
    return balance;
  }

  /**
   * Set the balance.
   * 
   * @param balance
   *          the balance to set
   */
  public void setBalance(AccountBalance balance) {
    this.balance = balance;
  }

}
