/* ==================================================================
 * AbstractMyBatisTest.java - 4/08/2020 6:50:51 AM
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.runner.RunWith;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.context.junit4.SpringRunner;

import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.billing.snf.domain.Address;

/**
 * Base class for MyBatis tests.
 * 
 * @author matt
 * @version 1.0
 */
@RunWith(SpringRunner.class)
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractMyBatisTest {

  protected final Logger log = LoggerFactory.getLogger(getClass());

  @Autowired
  private SqlSessionTemplate sqlSessionTemplate;

  @Autowired
  private JdbcOperations jdbcTemplate;

  protected SqlSessionTemplate getSqlSessionTemplate() {
    return sqlSessionTemplate;
  }

  /**
   * Create a test address instance.
   * 
   * @return the address
   */
  protected Address createTestAddress() {
    Address s = new Address(null, Instant.ofEpochMilli(System.currentTimeMillis()));
    s.setName("Tester Dude");
    s.setEmail("test@localhost");
    s.setCountry("NZ");
    s.setTimeZoneId("Pacific/Auckland");
    s.setRegion("Region");
    s.setStateOrProvince("State");
    s.setLocality("Wellington");
    s.setPostalCode("1001");
    s.setStreet(new String[] { "Level 1", "123 Main Street" });
    return s;
  }

  /**
   * Create a test account for a given address.
   * 
   * @param address
   *          the address
   * @return the account
   */
  protected Account createTestAccount(Address address) {
    Account account = new Account(null, UUID.randomUUID().getMostSignificantBits(),
        Instant.ofEpochMilli(System.currentTimeMillis()));
    account.setAddress(address);
    account.setCurrencyCode("NZD");
    account.setLocale("en_NZ");
    return account;
  }

  protected void debugQuery(String query) {
    StringBuilder buf = new StringBuilder();
    buf.append("Query ").append(query).append(":\n");
    for (Map<String, Object> row : jdbcTemplate.queryForList(query)) {
      buf.append(row).append("\n");
    }
    log.debug(buf.toString());
  }

  protected void debugRows(String table, String sort) {
    StringBuilder buf = new StringBuilder();
    buf.append("Table ").append(table).append(":\n");
    for (Map<String, Object> row : rows(table, sort)) {
      buf.append(row).append("\n");
    }
    log.debug(buf.toString());
  }

  protected List<Map<String, Object>> rows(String table) {
    return rows(table, "id");
  }

  protected List<Map<String, Object>> rows(String table, String sort) {
    StringBuilder buf = new StringBuilder("select * from ");
    buf.append(table);
    if (sort != null) {
      buf.append(" order by ").append(sort);
    }
    return jdbcTemplate.queryForList(buf.toString());
  }
}
