/* ==================================================================
 * ServiceConfig.java - 5/08/2020 9:46:02 PM
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

import org.snf.accounting.impl.DefaultAccountService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Service configuration.
 * 
 * @author matt
 * @version 1.0
 */
@Configuration
@Import(DefaultAccountService.class)
public class ServiceConfig {

}
