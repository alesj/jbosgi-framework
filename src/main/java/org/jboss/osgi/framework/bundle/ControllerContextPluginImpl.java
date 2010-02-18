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
package org.jboss.osgi.framework.bundle;

//$Id$

import java.util.Collections;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

import org.jboss.dependency.spi.ControllerContext;
import org.jboss.deployers.structure.spi.DeploymentRegistry;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.internal.AbstractOSGiMetaData;
import org.jboss.osgi.framework.plugins.ControllerContextPlugin;
import org.jboss.osgi.framework.plugins.internal.AbstractPlugin;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

/**
 * A plugin that manages OSGi services
 * 
 * @author thomas.diesler@jboss.com
 * @since 20-Jan-2010
 */
public class ControllerContextPluginImpl extends AbstractPlugin implements ControllerContextPlugin
{
   // Provide logging
   final Logger log = Logger.getLogger(ControllerContextPluginImpl.class);

   /** The deployment registry */
   private DeploymentRegistry registry;

   public ControllerContextPluginImpl(OSGiBundleManager bundleManager, DeploymentRegistry registry)
   {
      super(bundleManager);

      if (registry == null)
         throw new IllegalArgumentException("Null deployment registry");

      this.registry = registry;
   }

   public DeploymentUnit putContext(ControllerContext context, DeploymentUnit unit)
   {
      return registry.putContext(context, unit);
   }

   public DeploymentUnit removeContext(ControllerContext context, DeploymentUnit unit)
   {
      return registry.removeContext(context, unit);
   }

   public Set<ControllerContext> getRegisteredContexts(AbstractBundleState bundleState)
   {
      if (bundleState instanceof OSGiBundleState == false)
         return Collections.emptySet();

      DeploymentUnit unit = ((OSGiBundleState)bundleState).getDeploymentUnit();
      return registry.getContexts(unit);
   }

   /**
    * Get bundle for user tracker.
    *
    * @param user the user tracker object
    * @return bundle state
    */
   public AbstractBundleState getBundleForUser(Object user)
   {
      if (user instanceof AbstractBundleState)
         return (AbstractBundleState)user;
      else if (user instanceof ControllerContext)
         return getBundleForContext((ControllerContext)user);
      else
         throw new IllegalArgumentException("Unknown tracker type: " + user);
   }

   /**
    * Get bundle for context.
    *
    * @param context the context
    * @return bundle state
    */
   public AbstractBundleState getBundleForContext(ControllerContext context)
   {
      if (context instanceof OSGiServiceState)
      {
         OSGiServiceState service = (OSGiServiceState)context;
         return service.getBundleState();
      }

      OSGiBundleManager bundleManager = getBundleManager();
      DeploymentUnit unit = registry.getDeployment(context);
      if (unit != null)
      {
         synchronized (unit)
         {
            OSGiBundleState bundleState = unit.getAttachment(OSGiBundleState.class);
            if (bundleState == null)
            {
               OSGiMetaData osgiMetaData = unit.getAttachment(OSGiMetaData.class);
               if (osgiMetaData == null)
               {
                  Manifest manifest = unit.getAttachment(Manifest.class);
                  // [TODO] we need a mechanism to construct an OSGiMetaData from an easier factory
                  if (manifest == null)
                     manifest = new Manifest();
                  // [TODO] populate some bundle information
                  Attributes attributes = manifest.getMainAttributes();
                  attributes.put(new Name(Constants.BUNDLE_SYMBOLICNAME), unit.getName());
                  osgiMetaData = new AbstractOSGiMetaData(manifest);
                  unit.addAttachment(OSGiMetaData.class, osgiMetaData);
               }

               try
               {
                  bundleState = (OSGiBundleState)bundleManager.addDeployment(unit);
                  bundleManager.addBundle(bundleState);
                  bundleState.startInternal();
               }
               catch (Throwable t)
               {
                  throw new RuntimeException("Cannot dynamically add generic bundle: " + unit, t);
               }
            }
            return bundleState;
         }
      }

      return bundleManager.getSystemBundle();
   }

   /**
    * Unregister contexts.
    *
    * @param bundleState the stopping bundle
    */
   public void unregisterContexts(AbstractBundleState bundleState)
   {
      if (bundleState instanceof OSGiBundleState)
      {
         DeploymentUnit unit = ((OSGiBundleState)bundleState).getDeploymentUnit();
         Set<ControllerContext> contexts = registry.getContexts(unit);
         for (ControllerContext context : contexts)
         {
            if (context instanceof ServiceRegistration)
            {
               ServiceRegistration service = (ServiceRegistration)context;
               service.unregister();
            }
         }
      }
   }
}