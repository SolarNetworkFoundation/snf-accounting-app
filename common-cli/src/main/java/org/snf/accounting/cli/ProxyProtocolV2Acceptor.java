/* ==================================================================
 * ProxyProtocolV2Acceptor.java - 23/09/2020 4:05:17 pm
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
import java.net.UnknownHostException;

import javax.cache.Cache;

import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.buffer.BufferUtils;
import org.apache.sshd.common.util.logging.AbstractLoggingBean;
import org.apache.sshd.server.session.AbstractServerSession;
import org.apache.sshd.server.session.ServerProxyAcceptor;
import org.apache.sshd.server.session.ServerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ServerProxyAcceptor} for the Proxy Protocol V2.
 * 
 * @author matt
 * @version 1.0
 */
public class ProxyProtocolV2Acceptor extends AbstractLoggingBean implements ServerProxyAcceptor {

  private static final byte PROTOCOL_VERS_2 = (byte) 0x2;

  private static final byte ADDR_FAMILY_IP4 = (byte) 0x1;
  private static final byte ADDR_FAMILY_IP6 = (byte) 0x2;

  private static final byte COMMAND_LOCAL = (byte) 0x0;
  private static final byte COMMAND_PROXY = (byte) 0x1;

  private static final int PROXY_HEADER_LEN = 16;
  private static final byte[] PROXY_HEADER_SIG = new byte[] { 0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D,
      0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A };

  private static final Logger log = LoggerFactory.getLogger(ProxyProtocolV2Acceptor.class);

  private final Cache<InetAddress, Byte> denyList;

  /**
   * Constructor.
   * 
   * @param denyList
   *          a deny list to use
   */
  public ProxyProtocolV2Acceptor(Cache<InetAddress, Byte> denyList) {
    super();
    this.denyList = denyList;
  }

  @Override
  public boolean acceptServerProxyMetadata(ServerSession session, Buffer buffer) throws Exception {
    final int mark = buffer.rpos();
    int dataLen = buffer.available();
    if (dataLen < PROXY_HEADER_LEN) {
      if (log.isDebugEnabled()) {
        log.debug("{} incomplete data - {}/{}", session, dataLen, PROXY_HEADER_LEN);
      }
      return false;
    }

    byte[] proxyHeader = new byte[PROXY_HEADER_LEN];
    buffer.getRawBytes(proxyHeader);

    for (int i = 0; i < PROXY_HEADER_SIG.length; i++) {
      if (PROXY_HEADER_SIG[i] != proxyHeader[i]) {
        if (log.isDebugEnabled()) {
          // CHECKSTYLE OFF: LineLength
          log.debug("{} mismatched protocol signature: expected={}, actual={}", session,
              BufferUtils.toHex(':', PROXY_HEADER_SIG), BufferUtils.toHex(':', proxyHeader));
          // CHECKSTYLE ON: LineLength
        }
        buffer.rpos(mark); // Rewind the buffer
        return true;
      }
    }

    final int addrLen = ((Byte.toUnsignedInt(proxyHeader[14]) << 8)
        | Byte.toUnsignedInt(proxyHeader[15]));
    final byte[] addr = new byte[addrLen];
    if (addrLen > 0) {
      buffer.getRawBytes(addr);
    }

    final byte vers = (byte) ((proxyHeader[12] >>> 4) & 0xF);
    if (vers != PROTOCOL_VERS_2) {
      if (log.isDebugEnabled()) {
        // CHECKSTYLE OFF: LineLength
        log.debug("{} mismatched protocol version: expected={}, actual={}", session,
            BufferUtils.toHex(':', PROTOCOL_VERS_2), BufferUtils.toHex(':', vers));
        // CHECKSTYLE ON: LineLength
      }
      return true;
    }

    final byte cmd = (byte) (proxyHeader[12] & 0xF);
    if (cmd == COMMAND_LOCAL) {
      return true;
    } else if (cmd != COMMAND_PROXY) {
      if (log.isDebugEnabled()) {
        // CHECKSTYLE OFF: LineLength
        log.debug("{} unsupported command: expected={}, actual={}", session,
            BufferUtils.toHex(':', COMMAND_PROXY), BufferUtils.toHex(':', cmd));
        // CHECKSTYLE ON: LineLength
      }
      return true;
    }

    final byte addrFamily = (byte) ((proxyHeader[13] >>> 4) & 0xF);
    if (addrFamily < (byte) 0x1 || addrFamily > (byte) 0x3) {
      if (addrFamily > (byte) 0x0 && log.isDebugEnabled()) {
        // CHECKSTYLE OFF: LineLength
        log.debug("{} unsupported address family: actual={}", session,
            BufferUtils.toHex(':', addrFamily));
        // CHECKSTYLE ON: LineLength
      }
      return true;
    }

    final byte proto = (byte) (proxyHeader[13] & 0xF);
    if (proto < (byte) 0x0 || proto > (byte) 0x2) {
      if (log.isDebugEnabled()) {
        // CHECKSTYLE OFF: LineLength
        log.debug("{} unsupported transport protocol: actual={}", session,
            BufferUtils.toHex(':', proto));
        // CHECKSTYLE ON: LineLength
      }
      return true;
    }

    InetSocketAddress clientAddr = parseClientAddress(session, addrFamily, proto, addr);
    if (clientAddr != null && session instanceof AbstractServerSession) {
      // Set the client address in the session from the proxy payload
      ((AbstractServerSession) session).setClientAddress(clientAddr);

      if (denyList != null && denyList.containsKey(clientAddr.getAddress())) {
        log.info("{} connection blocked via brute force filter", clientAddr.getAddress());
        throw new IOException("Blocked.");
      }
    }

    return true;
  }

  private byte[] slice(byte[] src, int pos, int len) {
    byte[] result = new byte[len];
    System.arraycopy(src, pos, result, 0, len);
    return result;
  }

  private InetSocketAddress parseClientAddress(ServerSession session, byte addrFamily, byte proto,
      byte[] addr) {
    InetAddress srcAddress = null;
    final int len = (addrFamily == ADDR_FAMILY_IP4 ? 4 : 16);
    if (addrFamily == ADDR_FAMILY_IP4 || addrFamily == ADDR_FAMILY_IP6) {
      byte[] srcAddr = slice(addr, 0, len);
      try {
        srcAddress = InetAddress.getByAddress(srcAddr);
      } catch (UnknownHostException e) {
        if (log.isDebugEnabled()) {
          // CHECKSTYLE OFF: LineLength
          log.debug("{} invalid IP address length: expected={} actual={}", session,
              (addrFamily == ADDR_FAMILY_IP4 ? 4 : 16), BufferUtils.toHex(':', addr));
          // CHECKSTYLE ON: LineLength
        }
        return null;
      }
    } else {
      if (log.isDebugEnabled()) {
        // CHECKSTYLE OFF: LineLength
        log.debug("{} unsupported address family: actual={}", session,
            BufferUtils.toHex(':', addr));
        // CHECKSTYLE ON: LineLength
      }
      return null;
    }

    final int port = (Byte.toUnsignedInt(addr[len * 2]) << 8)
        | Byte.toUnsignedInt(addr[len * 2 + 1]);
    return new InetSocketAddress(srcAddress, port);
  }

}
