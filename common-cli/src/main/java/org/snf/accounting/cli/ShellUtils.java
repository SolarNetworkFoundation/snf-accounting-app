/* ==================================================================
 * ShellUtils.java - 3/08/2020 11:41:27 AM
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

import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.Locale;
import java.util.Map;

import org.apache.sshd.server.Environment;
import org.davidmoten.text.utils.WordWrap;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

import com.github.fonimus.ssh.shell.PromptColor;
import com.github.fonimus.ssh.shell.SshContext;

/**
 * Utilities for the CLI shell.
 * 
 * @author matt
 * @version 1.0
 */
public class ShellUtils {

  /** A default maximum width of shell output, i.e. for wrapping. */
  public static final int SHELL_MAX_COLS = 80;

  // @formatter:off
  /** A date formatter for YYYY-MM style months. */
  public static final DateTimeFormatter ISO_MONTH = new DateTimeFormatterBuilder()
      .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
      .appendLiteral('-')
      .appendValue(MONTH_OF_YEAR, 2)
      .toFormatter();
  // @formatter:off  

  /**
   * Color a bold message.
   *
   * @param message
   *          message to return
   * @return colored message
   */
  public static String getBold(String message) {
    return new AttributedStringBuilder().append(message, AttributedStyle.BOLD).toAnsi();
  }

  /**
   * Color a bold message with given color.
   *
   * @param message
   *          message to return
   * @param color
   *          color to print
   * @return colored message
   */
  public static String getBoldColored(String message, PromptColor color) {
    return new AttributedStringBuilder()
        .append(message, AttributedStyle.BOLD.foreground(color.toJlineAttributedStyle())).toAnsi();
  }

  /**
   * Color a faint message.
   *
   * @param message
   *          message to return
   * @return colored message
   */
  public static String getFaint(String message) {
    return new AttributedStringBuilder().append(message, AttributedStyle.DEFAULT.faint()).toAnsi();
  }

  /**
   * Broadcast a message to all registered SSH shells.
   * 
   * @param message
   *          the message to broadcast
   */
  public static void wall(String message) {
    for (SshContext ctx : TrackingSshShellCommandFactory.sshContexts()) {
      ctx.getTerminal().writer().println(message);
      ctx.getTerminal().flush();
    }
  }

  /**
   * Wrap a string to a maximum character column width.
   * 
   * @param message
   *          the message to wrap
   * @param maxColumns
   *          the maximum number of characters wide to wrap the text at
   * @return the message with newline characters inserted where needed to wrap the text to at most
   *         {@code maxColumns} characters wide
   */
  public static String wrap(CharSequence message, int maxColumns) {
    return WordWrap.from(message).maxWidth(maxColumns).wrap();
  }

  /**
   * Wrap a string to the default character column width.
   * 
   * @param message
   *          the message to wrap
   * @return the message with newline characters inserted where needed to wrap the text to at most
   *         {@link #SHELL_MAX_COLS} characters wide
   */
  public static String wrap(CharSequence message) {
    return wrap(message, SHELL_MAX_COLS);
  }

  /**
   * Get the locale of the client user.
   * 
   * @return the locale, or {@literal null} if not known
   */
  public static Locale clientLocale() {
    Environment sshEnv = TrackingSshShellCommandFactory.sshEnvironment();
    if (sshEnv != null) {
      Map<String, String> env = sshEnv.getEnv();
      if (env != null) {
        String lang = env.get("LANG");
        if (lang != null) {
          // spit the likes of en_NZ.UTF-8
          String[] components = lang.split("\\.");
          String[] countryLang = components[0].split("_");
          if (countryLang.length > 1) {
            return new Locale(countryLang[0], countryLang[1]);
          } else {
            return new Locale(countryLang[0]);
          }
        }
      }
    }
    return null;
  }

}
