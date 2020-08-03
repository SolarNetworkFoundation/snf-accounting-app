/* ==================================================================
 * Utils.java - 3/08/2020 11:27:55 AM
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

package org.snf.accounting.util;

import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.util.FileCopyUtils;

/**
 * General utilities.
 * 
 * @author matt
 * @version 1.0
 */
public final class Utils {

  private Utils() {
    // can't construct me
  }

  /**
   * Get the contents of a UTF-8 ClassPath resource as a string.
   * 
   * @param resourceName
   *          the resource name
   * @param myClass
   *          the class to get the resource for
   * @return the resource content
   * @throws RuntimeException
   *           if the resource cannot be loaded for any reason
   */
  public static String getResource(String resourceName, Class<?> myClass) {
    try {
      return FileCopyUtils
          .copyToString(new InputStreamReader(myClass.getResourceAsStream(resourceName), "UTF-8"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
