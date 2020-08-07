/* ==================================================================
 * MyBatisAccountDaoTests.java - 4/08/2020 6:44:10 AM
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

package org.snf.accounting.dao.mybatis.test;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Test;
import org.snf.accounting.dao.mybatis.MyBatisAccountDao;
import org.snf.accounting.dao.mybatis.MyBatisAddressDao;
import org.snf.accounting.domain.AccountFilter;
import org.snf.accounting.domain.AccountWithBalance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;
import net.solarnetwork.central.user.domain.UserLongPK;
import net.solarnetwork.dao.FilterResults;

/**
 * Test cases for the {@link MyBatisAccountDao} class.
 * 
 * @author matt
 * @version 1.0
 */
@Import({ MyBatisAddressDao.class, MyBatisAccountDao.class })
public class MyBatisAccountDaoTests extends AbstractMyBatisTest {

  @Autowired
  private MyBatisAddressDao addressDao;

  @Autowired
  private MyBatisAccountDao dao;

  private Address address;
  private Account last;

  @Before
  public void setUp() throws Exception {
    address = addressDao.get(addressDao.save(createTestAddress()));
    last = null;
  }

  @Test
  public void insert() {
    Account entity = createTestAccount(address);
    UserLongPK pk = dao.save(entity);
    getSqlSessionTemplate().flushStatements();
    assertThat("PK created", pk, notNullValue());
    assertThat("PK userId preserved", pk.getUserId(), equalTo(entity.getUserId()));
    last = entity;
    last.getId().setId(pk.getId());
  }

  @Test
  public void getByPK() {
    insert();
    Account entity = dao.get(last.getId());

    assertThat("ID", entity.getId(), equalTo(last.getId()));
    assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
    assertThat("Account", entity.isSameAs(last), equalTo(true));
  }

  @Test
  public void update() {
    insert();
    Account obj = dao.get(last.getId());
    obj.setCurrencyCode("USD");
    obj.setLocale("en_US");
    UserLongPK pk = dao.save(obj);
    assertThat("PK unchanged", pk, equalTo(obj.getId()));

    Account entity = dao.get(pk);
    assertThat("Entity updated", entity.isSameAs(obj), equalTo(true));
  }

  @Test
  public void delete() {
    insert();
    dao.delete(last);
    assertThat("No longer found", dao.get(last.getId()), nullValue());
  }

  @Test
  public void delete_noMatch() {
    insert();
    Account someAddr = createTestAccount(address);
    dao.delete(someAddr);

    Account entity = dao.get(last.getId());
    assertThat("Entity unchanged", entity.isSameAs(last), equalTo(true));
  }

  private List<Account> setupTestAccounts(int count) {
    List<Account> results = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      Address addr = addressDao.get(addressDao.save(createTestAddress("test" + i + "@localhost")));
      Account acct = createTestAccount(addr);
      acct.setUserId((long) i);
      acct = dao.get(dao.save(acct));
      insertAccountBalance(acct.getId().getId(), new BigDecimal(String.valueOf(i * 3 + 3)),
          new BigDecimal(String.valueOf(i * 3 + 2)), new BigDecimal(String.valueOf(i * 3 + 1)));
      results.add(acct);
    }
    return results;
  }

  @Test
  public void filterForEmail_sortDefault() {
    // GIVEN
    List<Account> accounts = setupTestAccounts(5);

    // WHEN
    AccountFilter filter = new AccountFilter();
    filter.setEmail("test");
    FilterResults<AccountWithBalance, UserLongPK> result = dao.findFilteredBalances(filter, null,
        null, null);

    // THEN
    assertThat("Result returned", result, notNullValue());
    assertThat("Returned result count", result.getReturnedResultCount(), equalTo(accounts.size()));
    assertThat("Total results provided", result.getTotalResults(), equalTo((long) accounts.size()));

    List<AccountWithBalance> expectedEntities = accounts.stream()
        .map(e -> new AccountWithBalance(e)).collect(toList());

    List<AccountWithBalance> balances = StreamSupport.stream(result.spliterator(), false)
        .collect(toList());
    assertThat("Returned results", balances, hasSize(expectedEntities.size()));
    for (int i = 0; i < 4; i++) {
      AccountWithBalance balance = balances.get(i);
      AccountWithBalance expected = expectedEntities.get(i);
      assertThat(format("AccountWithBalance %d returned in order", i), balance, equalTo(expected));
      // TODO assertThat(format("AccountWithBalance %d data preserved", i),
      // balance.isSameAs(expected), equalTo(true));
    }
  }

}
