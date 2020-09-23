/* ==================================================================
 * BruteForceSshShellAuthenticationProvider.java - 23/09/2020 2:28:53 pm
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

import java.net.InetAddress;
import java.net.InetSocketAddress;

import javax.cache.Cache;

import org.apache.sshd.common.RuntimeSshException;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fonimus.ssh.shell.auth.SshShellAuthenticationProvider;

/**
 * {@link SshShellAuthenticationProvider} to integrate with brute force deny list.
 * 
 * @author matt
 * @version 1.0
 */
public class BruteForceSshShellAuthenticationProvider implements SshShellAuthenticationProvider {

  private static final Logger log = LoggerFactory.getLogger(BruteForceDenyEventListener.class);

  private final SshShellAuthenticationProvider delegate;
  private final Cache<InetAddress, Byte> denyList;
  private int maxFails = 1;

  /**
   * Constructor.
   * 
   * @param delegate
   *          the delegate authentication manager
   * @param denyList
   *          the deny list
   * @throws IllegalArgumentException
   *           if any argument is {@literal null}
   */
  public BruteForceSshShellAuthenticationProvider(SshShellAuthenticationProvider delegate,
      Cache<InetAddress, Byte> denyList) {
    super();
    if (delegate == null) {
      throw new IllegalArgumentException("The delegate argument must not be null.");
    }
    this.delegate = delegate;
    if (denyList == null) {
      throw new IllegalArgumentException("The denyList argument must not be null.");
    }
    this.denyList = denyList;
  }

  @Override
  public boolean authenticate(String username, String password, ServerSession session)
      throws PasswordChangeRequiredException, AsyncAuthException {
    boolean result = delegate.authenticate(username, password, session);
    if (!result) {
      if (session.getClientAddress() instanceof InetSocketAddress) {
        InetAddress src = ((InetSocketAddress) session.getClientAddress()).getAddress();
        if (!src.isLoopbackAddress()) {
          Byte count = denyList.get(src);
          if (count == null) {
            count = (byte) 1;
          } else if (count.byteValue() != (byte) 0xFF) {
            count = (byte) ((count.byteValue() & 0xFF) + 1);
          }
          log.info("{} authentication attempt [{}] failed: attempt {}", src, username,
              Byte.toUnsignedInt(count));
          denyList.put(src, count);
          if (Byte.toUnsignedInt(count) >= maxFails) {
            log.info("{} authentication attempt [{}] blocked after {} attempts", src, username,
                Byte.toUnsignedInt(count));
            session.close(false);
            throw new RuntimeSshException("Blocked.");
          }
        }
      }
    }
    return result;
  }

  /**
   * Get the max fails count.
   * 
   * @return the max fails before closing connection; defaults to {@literal 1}
   */
  public int getMaxFails() {
    return maxFails;
  }

  /**
   * Set the max fails count.
   * 
   * @param maxFails
   *          the count to set
   */
  public void setMaxFails(int maxFails) {
    this.maxFails = maxFails;
  }

}
