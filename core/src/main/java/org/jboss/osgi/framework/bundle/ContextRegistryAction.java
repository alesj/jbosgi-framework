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

import org.jboss.dependency.spi.Controller;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.tracker.ContextRegistry;

/**
 * Register osgi service into context registry.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
class ContextRegistryAction extends SimpleOSGiServiceAction
{
   /**
    * Get context registry.
    *
    * @param context the context
    * @return context registry or null if cannot be applied
    */
   protected ContextRegistry getContextRegistry(ControllerContext context)
   {
      Controller controller = context.getController();
      return (controller instanceof ContextRegistry) ?  ContextRegistry.class.cast(controller) : null;
   }

   /**
    * Register / unregister contex under exposed classes to context registry.
    *
    * @param context the context
    * @param register the registry
    * @throws Throwable for any error
    */
   protected void handleContext(OSGiServiceState context, boolean register) throws Throwable
   {
      ContextRegistry registry = getContextRegistry(context);
      if (registry != null)
      {
         ClassLoader cl = context.getClassLoader();
         String[] classes = context.getClasses();
         for (String clazz : classes)
         {
            Class<?> exposedClass = cl.loadClass(clazz);
            if (register)
               registry.registerInstantiatedContext(context, exposedClass);
            else
               registry.unregisterInstantiatedContext(context, exposedClass);
         }
      }
   }
}