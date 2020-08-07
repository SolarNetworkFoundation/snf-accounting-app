/* ==================================================================
 * MyBatisAddressDaoTests.java - 4/08/2020 6:59:03 AM
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.snf.accounting.dao.mybatis.MyBatisAddressDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import net.solarnetwork.central.user.billing.snf.domain.Address;

/**
 * Test cases for the {@link MyBatisAddressDao}.
 * 
 * @author matt
 * @version 1.0
 */
@Import(MyBatisAddressDao.class)
public class MyBatisAddressDaoTests extends AbstractMyBatisTest {

  @Autowired
  private MyBatisAddressDao dao;

  private Address last;

  @Before
  public void setup() {
    last = null;
  }

  @After
  public void teardown() {
    getSqlSessionTemplate().flushStatements();
  }

  @Test
  public void insert() {
    Address entity = createTestAddress();
    Long pk = dao.save(entity);
    assertThat("PK preserved", pk, equalTo(entity.getId()));
    last = entity;
  }

  @Test
  public void insert_duplicate() {
    insert();
    Address entity = createTestAddress();
    dao.save(entity);
  }

  @Test
  public void getByPK() {
    insert();
    Address entity = dao.get(last.getId());

    assertThat("ID", entity.getId(), equalTo(last.getId()));
    assertThat("Created", entity.getCreated(), equalTo(last.getCreated()));
    assertThat("Address", entity.isSameAs(last), equalTo(true));
  }

  @Test
  public void update() {
    insert();
    Address obj = dao.get(last.getId());
    obj.setName("Tester Dudette");
    obj.setEmail("test2@localhost");
    obj.setCountry("US");
    obj.setTimeZoneId("America/Los_Angeles");
    obj.setRegion("R");
    obj.setStateOrProvince("CA");
    obj.setLocality("SF");
    obj.setPostalCode("94114");
    Long pk = dao.save(obj);
    assertThat("PK unchanged", pk, equalTo(obj.getId()));

    Address entity = dao.get(pk);
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
    Address someAddr = new Address(UUID.randomUUID().getMostSignificantBits(), Instant.now());
    dao.delete(someAddr);

    Address entity = dao.get(last.getId());
    assertThat("Entity unchanged", entity.isSameAs(last), equalTo(true));
  }

}
