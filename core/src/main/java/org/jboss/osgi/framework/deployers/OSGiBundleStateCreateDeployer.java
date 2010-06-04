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

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.internal.OSGiManifestMetaData;

/**
 * A deployer that creates the {@link AbstractBundleState} through the {@link OSGiBundleManager}.
 * 
 * The bundle is not yet INSTALLED.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @author thomas.diesler@jboss.com
 */
public class OSGiBundleStateCreateDeployer extends AbstractRealDeployer
{
   /** The bundle manager */
   protected OSGiBundleManager bundleManager;

   public OSGiBundleStateCreateDeployer(OSGiBundleManager bundleManager)
   {
      if (bundleManager == null)
         throw new IllegalArgumentException("Null bundle manager");
      this.bundleManager = bundleManager;

      setInput(OSGiMetaData.class);
      setOutput(AbstractBundleState.class);
      setStage(DeploymentStages.POST_PARSE);
      setTopLevelOnly(true);
   }

   @Override
   public void internalDeploy(DeploymentUnit unit) throws DeploymentException
   {
      // Bundles that are based on a valid OSGi Manifest can 
      // move forward to DESCRIBE stage. After that, they must be started explicitly
      if (unit.isAttachmentPresent(OSGiManifestMetaData.class))
         unit.setRequiredStage(DeploymentStages.DESCRIBE);

      // Create the bundle state
      bundleManager.addDeployment(unit);
   }
}