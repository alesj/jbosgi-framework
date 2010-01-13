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
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.metadata.OSGiMetaData;

/**
 * OSGiBundleStateDeployer.<p>
 * 
 * This deployer creates a bundle state object for all top level bundle deployments.
 * 
 * Note: undeploy/remove part is in a separate deployer.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class OSGiBundleStateAddDeployer extends AbstractOSGiBundleStateDeployer
{
   /**
    * Create a new BundleStateDeployer.
    * 
    * @param bundleManager the bundleManager
    * @throws IllegalArgumentException for a null bundle manager
    */
   public OSGiBundleStateAddDeployer(OSGiBundleManager bundleManager)
   {
      super(bundleManager);
      setInput(OSGiMetaData.class);
   }

   @Override
   protected void internalDeploy(DeploymentUnit unit) throws DeploymentException
   {
      // [TODO] look at manifest headers and persistent state for this
      unit.setRequiredStage(DeploymentStages.DESCRIBE);
      
      bundleManager.addDeployment(unit);
   }
}
