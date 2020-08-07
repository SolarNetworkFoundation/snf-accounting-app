/* ==================================================================
 * ResultPaginationCommands.java - 6/08/2020 6:48:32 AM
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

import java.util.function.Consumer;

import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import com.github.fonimus.ssh.shell.commands.SshShellComponent;

import net.solarnetwork.domain.SimplePagination;

/**
 * Commands for result pagination.
 * 
 * @author matt
 * @version 1.0
 */
@SshShellComponent
@ShellCommandGroup("Result Pagination")
public class ResultPaginationCommands {

  // CHECKSTYLE OFF: LineLength
  private static final ThreadLocal<SimplePagination> FILTER = new ThreadLocal<>();
  private static final ThreadLocal<Consumer<? extends SimplePagination>> NAV_HANDLER = new ThreadLocal<>();
  // CHECKSTYLE ON: LineLength

  /**
   * Set the navigation results.
   * 
   * @param filter
   *          the filter that produced the results
   * @param handler
   *          the navigation handler
   */
  public static synchronized void setNavigationHandler(SimplePagination filter,
      Consumer<? extends SimplePagination> handler) {
    if (filter != null) {
      if (handler == null) {
        throw new IllegalArgumentException("The handler must not be null.");
      }
      FILTER.set(filter);
      NAV_HANDLER.set(handler);
    } else {
      FILTER.remove();
      NAV_HANDLER.remove();
    }
  }

  /**
   * Navigate to another page of results.
   * 
   * @param offset
   *          a relative offset (negative to go backwards)
   * @param page
   *          an absolute page to go to, if greater than {@literal 0}
   */
  @ShellMethod("Navigate to another page of query results.")
  @ShellMethodAvailability("nextPageAvailability")
  public void nextPage(
      @ShellOption(help = "Page offset from last result page (negative to go backwards).",
          defaultValue = "1") int offset,
      @ShellOption(help = "Absolute page to jump to, starting from 1.",
          defaultValue = "0") int page) {
    final SimplePagination lastFilter = FILTER.get();
    @SuppressWarnings({ "rawtypes", "unchecked" })
    final Consumer<SimplePagination> navigationHandler = (Consumer) NAV_HANDLER.get();
    if (lastFilter == null || navigationHandler == null || lastFilter.getMax() == null
        || lastFilter.getMax().intValue() < 1) {
      return;
    }
    final int pageSize = lastFilter.getMax().intValue();
    int nextPage = page;
    if (nextPage < 1) {
      final int currPage = ((lastFilter.getOffset() == null ? 0 : lastFilter.getOffset().intValue())
          / pageSize) + 1;
      nextPage = currPage + offset;
      if (nextPage < 1) {
        nextPage = 1;
      }
    }
    lastFilter.setOffset((nextPage - 1) * pageSize);
    navigationHandler.accept(lastFilter);
  }

  /**
   * Get the availability of the next-page command.
   * 
   * @return the availability
   */
  public Availability nextPageAvailability() {
    final SimplePagination lastFilter = FILTER.get();
    final Consumer<? extends SimplePagination> navigationHandler = NAV_HANDLER.get();
    if (lastFilter == null || navigationHandler == null) {
      return Availability.unavailable("No navigable results available.");
    }
    if (lastFilter.getMax() == null || lastFilter.getMax().intValue() < 1) {
      return Availability.unavailable("Results are not navigable.");
    }
    return Availability.available();
  }

}
