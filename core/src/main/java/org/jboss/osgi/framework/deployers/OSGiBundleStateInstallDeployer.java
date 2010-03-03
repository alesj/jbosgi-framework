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

import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;

/**
 * A deployer that adds the bundle state to the bundle manager.
 * 
 * This causes the bundle to get INSTALLED.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Feb-2010
 */
public class OSGiBundleStateInstallDeployer extends AbstractSimpleRealDeployer<AbstractBundleState>
{
   public OSGiBundleStateInstallDeployer()
   {
      super(AbstractBundleState.class);
      addInput(ClassLoadingMetaData.class);
      setStage(DeploymentStages.POST_PARSE);
      setTopLevelOnly(true);
   }

   @Override
   public void deploy(DeploymentUnit unit, AbstractBundleState bundleState) throws DeploymentException
   {
      // Add the bundle to the manager when it is metadata complete
      OSGiBundleManager bundleManager = bundleState.getBundleManager();
      bundleManager.addBundle(bundleState);
   }

   @Override
   public void undeploy(DeploymentUnit unit, AbstractBundleState bundleState)
   {
      // Remove the bundle from the manager
      OSGiBundleManager bundleManager = bundleState.getBundleManager();
      bundleManager.removeBundle(bundleState);
   }
}