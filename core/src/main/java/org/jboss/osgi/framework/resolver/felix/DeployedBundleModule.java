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
package org.jboss.osgi.framework.resolver.felix;

import java.util.ArrayList;
import java.util.List;

import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.metadata.Capability;
import org.jboss.classloading.spi.metadata.Requirement;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.DeployedBundleState;
import org.jboss.osgi.framework.classloading.OSGiCapability;
import org.jboss.osgi.framework.classloading.OSGiModule;
import org.jboss.osgi.framework.classloading.OSGiRequirement;

/**
 * A Resolver Module that is backed by a deployed bundle.
 *  
 * @author thomas.diesler@jboss.com
 * @since 31-May-2010
 */
class DeployedBundleModule extends AbstractBundleModule
{
   // Provide logging
   final Logger log = Logger.getLogger(DeployedBundleModule.class);

   private OSGiModule moduleDelegate;

   public DeployedBundleModule(DeployedBundleState bundleState)
   {
      super(bundleState);
   }

   @Override
   List<OSGiCapability> getModuleCapabilities()
   {
      List<OSGiCapability> result = new ArrayList<OSGiCapability>();
      List<Capability> mccaps = getModuleDelegate().getCapabilities();
      if (mccaps != null)
      {
         for (Capability mccap : mccaps)
         {
            if (mccap instanceof OSGiCapability)
            {
               result.add((OSGiCapability)mccap);
            }
            else
            {
               throw new IllegalArgumentException("Unsupported capability: " + mccap);
            }
            
         }
      }
      return result;
   }

   @Override
   List<OSGiRequirement> getModuleRequirements()
   {
      List<OSGiRequirement> result = new ArrayList<OSGiRequirement>();
      if (getModuleDelegate().getRequirements() != null)
      {
         for (Requirement mcreq : getModuleDelegate().getRequirements())
         {
            if (mcreq instanceof OSGiRequirement)
            {
               result.add((OSGiRequirement)mcreq);
            }
            else
            {
               throw new IllegalArgumentException("Unsupported requirement: " + mcreq);
            }
            
         }
      }
      return result;
   }

   private OSGiModule getModuleDelegate()
   {
      if (moduleDelegate == null)
      {
         DeployedBundleState bundleState = DeployedBundleState.assertBundleState(getBundle());
         moduleDelegate = (OSGiModule)bundleState.getDeploymentUnit().getAttachment(Module.class);
         if (moduleDelegate == null)
            throw new IllegalStateException("No OSGiModule attached to: " + bundleState);
      }
      return moduleDelegate;
   }
}