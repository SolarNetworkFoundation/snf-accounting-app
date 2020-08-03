/* ==================================================================
 * MyBatisAccountDao.java - 3/08/2020 4:29:21 PM
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

package org.snf.accounting.dao.mybatis;

import org.mybatis.spring.SqlSessionTemplate;
import org.snf.accounting.dao.AccountDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import net.solarnetwork.central.dao.mybatis.support.BaseMyBatisGenericDaoSupport;
import net.solarnetwork.central.user.billing.snf.domain.Account;
import net.solarnetwork.central.user.domain.UserLongPK;

/**
 * JDBC implementation of {@code AccountDao}.
 * 
 * @author matt
 * @version 1.0
 */
@Repository
public class MyBatisAccountDao extends BaseMyBatisGenericDaoSupport<Account, UserLongPK>
    implements AccountDao {

  /**
   * Constructor.
   * 
   * @param template
   *          the template
   */
  @Autowired
  public MyBatisAccountDao(SqlSessionTemplate template) {
    super(Account.class, UserLongPK.class);
    setSqlSessionTemplate(template);
  }

}
