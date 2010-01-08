/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.osgi.framework.service.internal;

//$Id$

import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.osgi.deployment.interceptor.AbstractLifecycleInterceptorService;
import org.jboss.osgi.deployment.interceptor.InvocationContext;
import org.jboss.osgi.deployment.interceptor.LifecycleInterceptorService;
import org.jboss.osgi.deployment.internal.InvocationContextImpl;
import org.jboss.osgi.framework.bundle.AbstractDeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.plugins.LifecycleInterceptorServicePlugin;
import org.jboss.osgi.framework.plugins.internal.AbstractServicePlugin;
import org.jboss.osgi.framework.util.DeploymentUnitAttachments;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * A plugin that manages bundle lifecycle interceptors.
 * 
 * @author thomas.diesler@jboss.com
 * @since 19-Oct-2009
 */
public class LifecycleInterceptorServiceImpl extends AbstractServicePlugin implements LifecycleInterceptorServicePlugin
{
   // Provide logging
   final Logger log = Logger.getLogger(LifecycleInterceptorServiceImpl.class);

   private AbstractLifecycleInterceptorService delegate;
   private ServiceRegistration registration;

   public LifecycleInterceptorServiceImpl(final OSGiBundleManager bundleManager)
   {
      super(bundleManager);
   }

   public void startService()
   {
      BundleContext sysContext = getSystemContext();
      delegate = new AbstractLifecycleInterceptorService(sysContext)
      {
         @Override
         protected InvocationContext getInvocationContext(Bundle bundle)
         {
            long bundleId = bundle.getBundleId();
            AbstractDeployedBundleState bundleState = (AbstractDeployedBundleState)bundleManager.getBundleById(bundleId);
            if (bundle == null)
               throw new IllegalStateException("Cannot obtain bundle for: " + bundle);

            VFSDeploymentUnit unit = (VFSDeploymentUnit)bundleState.getDeploymentUnit();
            InvocationContext inv = unit.getAttachment(InvocationContext.class);
            if (inv == null)
            {
               BundleContext context = bundleState.getBundleManager().getSystemContext();
               DeploymentUnitAttachments att = new DeploymentUnitAttachments(unit);
               inv = new InvocationContextImpl(context, bundle, unit.getRoot(), att);
               unit.addAttachment(InvocationContext.class, inv);
            }
            return inv;
         }
      };

      registration = sysContext.registerService(LifecycleInterceptorService.class.getName(), delegate, null);
   }

   public void stopService()
   {
      if (registration != null)
      {
         registration.unregister();
         registration = null;
         delegate = null;
      }
   }

   public void handleStateChange(int state, Bundle bundle)
   {
      if (delegate != null)
         delegate.handleStateChange(state, bundle);
   }
}