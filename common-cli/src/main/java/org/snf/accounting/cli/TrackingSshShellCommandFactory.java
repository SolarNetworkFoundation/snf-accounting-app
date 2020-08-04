/* ==================================================================
 * TrackingSshShellCommandFactory.java - 3/08/2020 11:42:15 AM
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.validation.ValidatorFactory;

import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.session.ServerSession;
import org.jline.reader.Parser;
import org.springframework.boot.Banner;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;
import org.springframework.shell.MethodTarget;
import org.springframework.shell.ParameterResolver;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.JLineShellAutoConfiguration.CompleterAdapter;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

import com.github.fonimus.ssh.shell.SshContext;
import com.github.fonimus.ssh.shell.SshShellCommandFactory;
import com.github.fonimus.ssh.shell.SshShellProperties;
import com.github.fonimus.ssh.shell.listeners.SshShellListenerService;

/**
 * Extension of {@link SshShellCommandFactory} to provide tracking of active SSH sessions.
 * 
 * @author matt
 * @version 1.0
 */
@Primary
@Component("trackingSshShellCommandFactory")
public class TrackingSshShellCommandFactory extends SshShellCommandFactory {

  private static final Map<SshContext, Boolean> SSH_CONTEXTS = new WeakHashMap<>(4, 0.9f);

  // CHECKSTYLE OFF: LineLength
  private static final Map<ServerSession, org.apache.sshd.server.Environment> SSH_ENVIRONMENTS = new WeakHashMap<>(
      4, 0.9f);
  // CHECKSTYLE ON: LineLength

  /**
   * Constructor.
   * 
   * @param shellListenerService
   *          shell listener service
   * @param banner
   *          shell banner
   * @param promptProvider
   *          prompt provider
   * @param shell
   *          spring shell
   * @param completerAdapter
   *          completer adapter
   * @param parser
   *          jline parser
   * @param environment
   *          spring environment
   * @param properties
   *          shell properties
   */
  public TrackingSshShellCommandFactory(SshShellListenerService shellListenerService, Banner banner,
      PromptProvider promptProvider, Shell shell, CompleterAdapter completerAdapter, Parser parser,
      Environment environment, SshShellProperties properties) {
    super(shellListenerService, banner, promptProvider, new TrackingShell(shell), completerAdapter,
        parser, environment, properties);
  }

  @Override
  public void start(ChannelSession channelSession, org.apache.sshd.server.Environment env) {
    super.start(channelSession, env);
    SSH_ENVIRONMENTS.put(channelSession.getServerSession(), env);
  }

  private static final class TrackingShell extends Shell {

    private final Shell delegate;

    private TrackingShell(Shell delegate) {
      super(null);
      this.delegate = delegate;
    }

    @Override
    public void setValidatorFactory(ValidatorFactory validatorFactory) {
      delegate.setValidatorFactory(validatorFactory);
    }

    @Override
    public Map<String, MethodTarget> listCommands() {
      return delegate.listCommands();
    }

    @Override
    public void gatherMethodTargets() throws Exception {
      delegate.gatherMethodTargets();
    }

    @Override
    public void setParameterResolvers(List<ParameterResolver> resolvers) {
      delegate.setParameterResolvers(resolvers);
    }

    @Override
    public void run(InputProvider inputProvider) throws IOException {
      // assumption that SSH_THREAD_CONTEXT has been set for the current thread at this point
      SshContext ctx = SSH_THREAD_CONTEXT.get();
      if (ctx != null) {
        synchronized (SSH_CONTEXTS) {
          SSH_CONTEXTS.put(ctx, Boolean.TRUE);
        }
      }
      delegate.run(inputProvider);
    }

    @Override
    public Object evaluate(Input input) {
      return delegate.evaluate(input);
    }

    @Override
    public List<CompletionProposal> complete(CompletionContext context) {
      return delegate.complete(context);
    }

  }

  /**
   * Get all available SSH contexts.
   * 
   * @return all available contexts
   */
  public static Iterable<SshContext> sshContexts() {
    synchronized (SSH_CONTEXTS) {
      Set<SshContext> keys = SSH_CONTEXTS.keySet();
      List<SshContext> contexts = new ArrayList<>(keys.size());
      keys.forEach(c -> {
        if (c != null) {
          contexts.add(c);
        }
      });
      return contexts;
    }
  }

  /**
   * Get the active SSH environment.
   * 
   * @return the active SSH environment
   */
  public static org.apache.sshd.server.Environment sshEnvironment() {
    SshContext ctx = SSH_THREAD_CONTEXT.get();
    if (ctx != null) {
      ServerSession sess = ctx.getSshSession();
      if (sess != null) {
        return SSH_ENVIRONMENTS.get(sess);
      }
    }
    return null;
  }

}
