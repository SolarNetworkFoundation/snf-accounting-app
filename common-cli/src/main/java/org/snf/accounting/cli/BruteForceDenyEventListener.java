/* ==================================================================
 * BruteForceDenyEventListener.java - 23/09/2020 10:45:27 am
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

package org.snf.accounting.cli;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import javax.cache.Cache;

import org.apache.sshd.common.io.IoAcceptor;
import org.apache.sshd.common.io.IoServiceEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link IoServiceEventListener} implementation for a dynamic "deny" firewall based on remote IP
 * addresses that fail to authenticate.
 * 
 * @author matt
 * @version 1.0
 */
public class BruteForceDenyEventListener implements IoServiceEventListener {

  private static final Logger log = LoggerFactory.getLogger(BruteForceDenyEventListener.class);

  private final Cache<InetAddress, Byte> denyList;

  /**
   * Constructor.
   * 
   * @param denyList
   *          the cache of blocked IP addresses
   * @throws IllegalArgumentException
   *           if any argument is {@literal null}
   */
  public BruteForceDenyEventListener(Cache<InetAddress, Byte> denyList) {
    super();
    if (denyList == null) {
      throw new IllegalArgumentException("The denyList argument must not be null.");
    }
    this.denyList = denyList;
  }

  @Override
  public void connectionAccepted(IoAcceptor acceptor, SocketAddress local, SocketAddress remote,
      SocketAddress service) throws IOException {
    if (remote instanceof InetSocketAddress) {
      InetAddress src = ((InetSocketAddress) remote).getAddress();
      if (denyList.containsKey(src)) {
        log.info("{} connection blocked via brute force filter", src);
        throw new IOException("Blocked.");
      }
    }
  }

}
