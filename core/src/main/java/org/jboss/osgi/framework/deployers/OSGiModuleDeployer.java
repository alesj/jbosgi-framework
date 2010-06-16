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
package org.jboss.osgi.framework.deployers;

import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.dependency.policy.ClassLoaderPolicyModule;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.plugins.classloader.VFSClassLoaderDescribeDeployer;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.classloading.OSGiClassLoadingMetaData;
import org.jboss.osgi.framework.classloading.OSGiModule;
import org.jboss.osgi.framework.plugins.ResolverPlugin;

/**
 * The OSGiModuleDeployer creates an {@link OSGiModule} if we have {@link OSGiClassLoadingMetaData}. 
 * 
 * @author thomas.diesler@jboss.com
 */
public class OSGiModuleDeployer extends VFSClassLoaderDescribeDeployer
{
   protected OSGiBundleManager bundleManager;

   public OSGiModuleDeployer(OSGiBundleManager bundleManager)
   {
      this.bundleManager = bundleManager;

      // Make sure we come first, otherwise a plain {@link Module} would be created
      // which is then not registered with the OSGi resolver.
      setRelativeOrder(Integer.MIN_VALUE);
   }

   @Override
   public void deploy(DeploymentUnit unit, ClassLoadingMetaData metaData) throws DeploymentException
   {
      super.deploy(unit, metaData);
      
      // Add the bundle to the resolver
      Module module = unit.getAttachment(Module.class);
      ResolverPlugin resolver = bundleManager.getOptionalPlugin(ResolverPlugin.class);
      if (resolver != null && module instanceof OSGiModule)
      {
         AbstractBundleState bundleState = unit.getAttachment(AbstractBundleState.class);
         resolver.addBundle(bundleState);
      }
   }

   @Override
   public void undeploy(DeploymentUnit unit, ClassLoadingMetaData deployment)
   {
      if (bundleManager != null)
      {
         // Remove the bundle from the resolver
         Module module = unit.getAttachment(Module.class);
         ResolverPlugin bundleResolver = bundleManager.getOptionalPlugin(ResolverPlugin.class);
         if (bundleResolver != null && module instanceof OSGiModule)
         {
            AbstractBundleState bundleState = unit.getAttachment(AbstractBundleState.class);
            bundleResolver.removeBundle(bundleState);
         }
      }
      super.undeploy(unit, deployment);
   }

   @Override
   protected ClassLoaderPolicyModule createModule(DeploymentUnit unit, ClassLoadingMetaData metaData) throws DeploymentException
   {
      ClassLoaderPolicyModule module;
      if (metaData instanceof OSGiClassLoadingMetaData)
      {
         module = new OSGiModule(unit, metaData);
      }
      else
      {
         module = super.createModule(unit, metaData);
      }
      return module;
   }
}
