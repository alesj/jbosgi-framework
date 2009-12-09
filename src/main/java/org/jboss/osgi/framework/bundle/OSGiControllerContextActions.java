/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.osgi.framework.bundle;

import java.util.HashMap;
import java.util.Map;

import org.jboss.dependency.plugins.AbstractControllerContextActions;
import org.jboss.dependency.plugins.action.ControllerContextAction;
import org.jboss.dependency.spi.ControllerContextActions;
import org.jboss.dependency.spi.ControllerState;

/**
 * OSGi actions.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
class OSGiControllerContextActions extends AbstractControllerContextActions
{
   static final ControllerContextActions ACTIONS = new OSGiControllerContextActions();

   private OSGiControllerContextActions()
   {
      super(getActions());
   }

   /**
    * Get actions.
    *
    * @return the actions
    */
   private static Map<ControllerState, ControllerContextAction> getActions()
   {
      Map<ControllerState, ControllerContextAction> map = new HashMap<ControllerState, ControllerContextAction>(2);
      map.put(ControllerState.PRE_INSTALL, new PreInstallAction());
      map.put(ControllerState.DESCRIBED, new DescribeAction());
      map.put(ControllerState.INSTANTIATED, new ContextRegistryAction());
      return map;
   }
}