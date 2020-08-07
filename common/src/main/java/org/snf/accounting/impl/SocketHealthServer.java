/* ==================================================================
 * SocketHealthServer.java - 7/08/2020 8:38:56 AM
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

package org.snf.accounting.impl;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple socket server to help with health checks.
 * 
 * <p>
 * This has been designed with AWS health checks in mind, so those checks do not cause excessive
 * error logging in the real application server.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
public class SocketHealthServer implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(SocketHealthServer.class);

  private final int port;

  /**
   * Constructor.
   * 
   * @port the port to listen on
   */
  public SocketHealthServer(int port) {
    super();
    this.port = port;
  }

  @Override
  public void run() {
    try (ServerSocket s = new ServerSocket(port, 2)) {
      log.info("Health socket server started [{}:{}]", s.getInetAddress().getHostAddress(),
          s.getLocalPort());
      while (true) {
        try (Socket sock = s.accept()) {
          log.debug("Health check accepted from {}", sock.getRemoteSocketAddress());
          // we don't care what client sends; we just send OK
          try (PrintWriter out = new PrintWriter(
              new OutputStreamWriter(sock.getOutputStream(), "UTF-8"))) {
            out.println("OK");
          }
        } catch (IOException e2) {
          // ignore this one
        }
      }
    } catch (IOException e) {
      log.warn("Error setting up health server: {}", e.toString());
    }
  }

}
