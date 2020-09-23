/* ==================================================================
 * SshConfig.java - 23/09/2020 1:03:15 pm
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

package org.snf.accounting.cli.app.config;

import java.io.IOException;
import java.net.InetAddress;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.cache.Cache;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.RejectAllPublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snf.accounting.cli.BruteForceDenyEventListener;
import org.snf.accounting.cli.ProxyProtocolV2Acceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.fonimus.ssh.shell.SshShellCommandFactory;
import com.github.fonimus.ssh.shell.SshShellProperties;
import com.github.fonimus.ssh.shell.auth.SshShellPublicKeyAuthenticationProvider;

/**
 * SSH server configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
public class SshConfig {

  private static final Logger LOGGER = LoggerFactory.getLogger(SshShellConfig.class);

  @Value("${app.ssh.proxy-protocol:false}")
  private boolean useProxyProtocol = false;

  private SshShellProperties properties;

  private SshShellCommandFactory shellCommandFactory;

  private PasswordAuthenticator passwordAuthenticator;

  @Autowired
  @Qualifier("brute-force-deny-list")
  private Cache<InetAddress, Byte> bruteForceDenyList;

  /**
   * Constructor.
   * 
   * @param properties
   *          the properties
   * @param shellCommandFactory
   *          the shell command factory
   * @param passwordAuthenticator
   *          the authenticator
   */
  public SshConfig(SshShellProperties properties, SshShellCommandFactory shellCommandFactory,
      PasswordAuthenticator passwordAuthenticator) {
    super();
    this.properties = properties;
    this.shellCommandFactory = shellCommandFactory;
    this.passwordAuthenticator = passwordAuthenticator;
  }

  /**
   * Start ssh server.
   *
   * @throws IOException
   *           in case of error
   */
  @PostConstruct
  public void startServer() throws IOException {
    sshServer().start();
    LOGGER.info("Ssh server started [{}:{}]", properties.getHost(), properties.getPort());
  }

  /**
   * Stop ssh server.
   *
   * @throws IOException
   *           in case of error
   */
  @PreDestroy
  public void stopServer() throws IOException {
    sshServer().stop();
  }

  private BruteForceDenyEventListener bruteForceEventListener() {
    return new BruteForceDenyEventListener(bruteForceDenyList);
  }

  /**
   * Construct ssh server thanks to ssh shell properties.
   *
   * @return ssh server
   */
  @Bean
  public SshServer sshServer() {
    SshServer server = SshServer.setUpDefaultServer();
    server.setKeyPairProvider(
        new SimpleGeneratorHostKeyProvider(properties.getHostKeyFile().toPath()));
    server.setHost(properties.getHost());
    server.setPasswordAuthenticator(passwordAuthenticator);
    server.setPublickeyAuthenticator(RejectAllPublickeyAuthenticator.INSTANCE);
    if (properties.getAuthorizedPublicKeysFile() != null) {
      if (properties.getAuthorizedPublicKeysFile().exists()
          && properties.getAuthorizedPublicKeysFile().canRead()) {
        server.setPublickeyAuthenticator(
            new SshShellPublicKeyAuthenticationProvider(properties.getAuthorizedPublicKeysFile()));
      } else {
        // CHECKSTYLE OFF: LineLength
        LOGGER.warn(
            "Could not read authorized public keys file [{}], public key authentication is disabled.",
            properties.getAuthorizedPublicKeysFile().getAbsolutePath());
        // CHECKSTYLE ON: LineLength
      }
    }
    server.setPort(properties.getPort());
    server.setShellFactory(channelSession -> shellCommandFactory);
    server.setCommandFactory((channelSession, s) -> shellCommandFactory);
    server.setIoServiceEventListener(bruteForceEventListener());
    if (useProxyProtocol) {
      LOGGER.info("Using proxy protocol acceptor.");
      server.setServerProxyAcceptor(new ProxyProtocolV2Acceptor(bruteForceDenyList));
    }
    return server;
  }

}
