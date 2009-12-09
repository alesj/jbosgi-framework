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

import org.jboss.dependency.plugins.action.SimpleControllerContextAction;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.spi.metadata.KernelMetaDataRepository;
import org.jboss.logging.Logger;

/**
 * Handle OSGi service context.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class OSGiServiceAction extends SimpleControllerContextAction<OSGiServiceState>
{
   protected Logger log = Logger.getLogger(getClass());
   
   protected OSGiServiceState contextCast(ControllerContext context)
   {
      return OSGiServiceState.class.cast(context);
   }

   protected boolean validateContext(ControllerContext context)
   {
      return (context instanceof OSGiServiceState);
   }

   /**
    * Get kernel.
    *
    * @param context the context
    * @return MC kernel
    */
   protected Kernel getKernel(OSGiServiceState context)
   {
      AbstractBundleState bundleState = context.getBundleState();
      OSGiBundleManager manager = bundleState.getBundleManager();
      return manager.getKernel();
   }

   /**
    * Get metadata repository.
    *
    * @param context the context
    * @return kernel metadata repository
    */
   protected KernelMetaDataRepository getRepository(OSGiServiceState context)
   {
      Kernel kernel = getKernel(context);
      return kernel.getMetaDataRepository();
   }
}