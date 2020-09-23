/* ==================================================================
 * SshShellConfig.java - 23/09/2020 10:30:52 am
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

import static com.github.fonimus.ssh.shell.SshShellCommandFactory.SSH_THREAD_CONTEXT;
import static com.github.fonimus.ssh.shell.SshShellProperties.SSH_SHELL_PREFIX;

import java.net.InetAddress;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.cache.Cache;

import org.jline.reader.LineReader;
import org.jline.reader.Parser;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.snf.accounting.cli.BruteForceSshShellAuthenticationProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.shell.ResultHandler;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.shell.standard.ValueProvider;

import com.github.fonimus.ssh.shell.ExtendedCompleterAdapter;
import com.github.fonimus.ssh.shell.ExtendedShell;
import com.github.fonimus.ssh.shell.SshContext;
import com.github.fonimus.ssh.shell.SshShellCommandFactory;
import com.github.fonimus.ssh.shell.SshShellHelper;
import com.github.fonimus.ssh.shell.SshShellProperties;
import com.github.fonimus.ssh.shell.SshShellTerminalDelegate;
import com.github.fonimus.ssh.shell.auth.SshShellAuthenticationProvider;
import com.github.fonimus.ssh.shell.auth.SshShellPasswordAuthenticationProvider;
import com.github.fonimus.ssh.shell.auth.SshShellSecurityAuthenticationProvider;
import com.github.fonimus.ssh.shell.listeners.SshShellListener;
import com.github.fonimus.ssh.shell.listeners.SshShellListenerService;
import com.github.fonimus.ssh.shell.postprocess.PostProcessor;
import com.github.fonimus.ssh.shell.postprocess.TypePostProcessorResultHandler;
import com.github.fonimus.ssh.shell.postprocess.provided.GrepPostProcessor;
import com.github.fonimus.ssh.shell.postprocess.provided.HighlightPostProcessor;
import com.github.fonimus.ssh.shell.postprocess.provided.JsonPointerPostProcessor;
import com.github.fonimus.ssh.shell.postprocess.provided.PrettyJsonPostProcessor;
import com.github.fonimus.ssh.shell.postprocess.provided.SavePostProcessor;
import com.github.fonimus.ssh.shell.providers.ExtendedFileValueProvider;

/**
 * Configuration for the SSH Shell.
 * 
 * <p>
 * Adapted from {@code com.github.fonimus.ssh.shell.SshShellAutoConfiguration} so that the SSH
 * server creation could be customized.
 * </p>
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@EnableConfigurationProperties({ SshShellProperties.class })
@Import(value = { SshShellCommandFactory.class })
@SuppressWarnings({ "rawtypes" })
public class SshShellConfig {

  private static final String TERMINAL_DELEGATE = "terminalDelegate";

  public ApplicationContext context;

  private ConfigurableEnvironment environment;

  private SshShellProperties properties;

  @Autowired
  @Qualifier("brute-force-deny-list")
  private Cache<InetAddress, Byte> bruteForceDenyList;

  /**
   * Constructor.
   * 
   * @param context
   *          the context
   * @param environment
   *          the environment
   * @param properties
   *          the shell properties
   */
  public SshShellConfig(ApplicationContext context, ConfigurableEnvironment environment,
      SshShellProperties properties) {
    this.context = context;
    this.environment = environment;
    this.properties = properties;
  }

  /**
   * Initialize ssh shell auto config.
   */
  @PostConstruct
  public void init() {
    if (!properties.getPrompt().getLocal().isEnable()) {
      InteractiveShellApplicationRunner.disable(environment);
    }
  }

  @Bean
  @Primary
  public Shell sshShell(@Qualifier("main") ResultHandler<Object> resultHandler,
      List<PostProcessor> postProcessors) {
    return new ExtendedShell(new TypePostProcessorResultHandler(resultHandler, postProcessors),
        postProcessors);
  }

  @Bean
  @Primary
  public ExtendedCompleterAdapter sshCompleter(Shell shell) {
    return new ExtendedCompleterAdapter(shell);
  }

  // value providers

  @Bean
  public ValueProvider extendedFileValueProvider() {
    return new ExtendedFileValueProvider(properties.isExtendedFileProvider());
  }

  // post processors

  @Bean
  @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
  public JsonPointerPostProcessor jsonPointerPostProcessor() {
    return new JsonPointerPostProcessor();
  }

  @Bean
  @ConditionalOnClass(name = "com.fasterxml.jackson.databind.ObjectMapper")
  public PrettyJsonPostProcessor prettyJsonPostProcessor() {
    return new PrettyJsonPostProcessor();
  }

  @Bean
  public SavePostProcessor savePostProcessor() {
    return new SavePostProcessor();
  }

  @Bean
  public GrepPostProcessor grepPostProcessor() {
    return new GrepPostProcessor();
  }

  @Bean
  public HighlightPostProcessor highlightPostProcessor() {
    return new HighlightPostProcessor();
  }

  @Bean
  public SshShellHelper sshShellHelper() {
    return new SshShellHelper(properties.getConfirmationWords());
  }

  /**
   * The security auth provider.
   * 
   * @return the provider
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnClass(name = "org.springframework.security.authentication.AuthenticationManager")
  @ConditionalOnProperty(value = SSH_SHELL_PREFIX + ".authentication", havingValue = "security")
  public SshShellAuthenticationProvider sshShellSecurityAuthenticationProvider() {
    SshShellSecurityAuthenticationProvider delegate = new SshShellSecurityAuthenticationProvider(
        context, properties.getAuthProviderBeanName());
    delegate.init();
    return new BruteForceSshShellAuthenticationProvider(delegate, bruteForceDenyList);
  }

  /**
   * The simple auth provider.
   * 
   * @return the provider
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(value = SSH_SHELL_PREFIX + ".authentication", havingValue = "simple",
      matchIfMissing = true)
  public SshShellAuthenticationProvider sshShellSimpleAuthenticationProvider() {
    return new BruteForceSshShellAuthenticationProvider(
        new SshShellPasswordAuthenticationProvider(properties.getUser(), properties.getPassword()),
        bruteForceDenyList);
  }

  /**
   * Primary terminal which delegates with right session.
   *
   * @param terminal
   *          jline terminal
   * @param lineReader
   *          jline line reader
   * @return terminal
   */
  @Bean(TERMINAL_DELEGATE)
  @Primary
  public Terminal terminal(Terminal terminal, LineReader lineReader) {
    if (properties.getPrompt().getLocal().isEnable()) {
      // local prompt enable, add ssh context in main thread
      SSH_THREAD_CONTEXT.set(new SshContext(null, terminal, lineReader, null));
    }
    return new SshShellTerminalDelegate(terminal);
  }

  /**
   * Primary prompt provider.
   *
   * @return prompt provider
   */
  @Bean
  @ConditionalOnMissingBean
  public PromptProvider sshPromptProvider() {
    return () -> new AttributedString(properties.getPrompt().getText(), AttributedStyle.DEFAULT
        .foreground(properties.getPrompt().getColor().toJlineAttributedStyle()));
  }

  /**
   * Creates ssh listener service.
   *
   * @param listeners
   *          found listeners in context
   * @return listener service
   */
  @Bean
  public SshShellListenerService sshShellListenerService(
      @Autowired(required = false) List<SshShellListener> listeners) {
    return new SshShellListenerService(listeners);
  }

  /**
   * Primary shell application runner which answers true to
   * {@link InteractiveShellApplicationRunner#isEnabled()}.
   *
   * @param lineReader
   *          line reader
   * @param promptProvider
   *          prompt provider
   * @param parser
   *          parser
   * @param shell
   *          spring shell
   * @param environment
   *          spring environment
   * @return shell application runner
   */
  @Bean
  @Primary
  public InteractiveShellApplicationRunner sshInteractiveShellApplicationRunner(
      LineReader lineReader, PromptProvider promptProvider, Parser parser, Shell shell,
      Environment environment) {
    return new InteractiveShellApplicationRunner(lineReader, promptProvider, parser, shell,
        environment) {

      @Override
      public boolean isEnabled() {
        return true;
      }

      @Override
      public void run(ApplicationArguments args) {
        // do nothing
      }
    };
  }

}
