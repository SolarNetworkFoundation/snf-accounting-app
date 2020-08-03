/* ==================================================================
 * BaseMessageSourceSupport.java - 3/08/2020 11:40:23 AM
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

import org.springframework.context.MessageSource;
import org.springframework.context.support.ResourceBundleMessageSource;

/**
 * Basic support for a service using a {@link MessageSource}.
 * 
 * @author matt
 * @version 1.0
 */
public abstract class BaseMessageSourceSupport {

  /** The message source. */
  protected MessageSource messageSource;

  /**
   * Constructor.
   * 
   * <p>
   * A default {@link ResourceBundleMessageSource} based on the instance's class name will be
   * created.
   * </p>
   */
  public BaseMessageSourceSupport() {
    super();
    ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
    ms.setBasenames(BaseMessageSourceSupport.class.getName(), getClass().getName());
    setMessageSource(ms);
  }

  /**
   * Constructor.
   */
  public BaseMessageSourceSupport(MessageSource messageSource) {
    super();
    setMessageSource(messageSource);
  }

  /**
   * Configure the message source.
   * 
   * @param messageSource
   *          the message source to set
   */
  public void setMessageSource(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

}
