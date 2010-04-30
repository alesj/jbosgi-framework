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

import org.jboss.classloading.spi.dependency.ClassLoading;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.classloading.OSGiClassLoadingMetaData;

/**
 * [TODO] Remove this deployer when the VFSClassLoaderDescribeDeployer 
 * suports an already attached Module.
 * 
 * [JBOSGI-317] Context is already registered in domain DefaultDomain
 * https://jira.jboss.org/jira/browse/JBOSGI-317
 * 
 * @author thomas.diesler@jboss.com
 */
public class OSGiModuleDeployerWorkaround extends OSGiModuleDeployer
{
   public OSGiModuleDeployerWorkaround()
   {
      // Make sure we come after the ClassLoaderDescribeDeployer
      setInput(Module.class);
   }

   @Override
   public void deploy(DeploymentUnit unit, ClassLoadingMetaData metaData) throws DeploymentException
   {
      // Do nothing if the workaround is not enabled
      if ("true".equals(bundleManager.getProperty("jbosgi317.workaround")) == false)
         return;
      
      // If there is already a module attached and this is an OSGi deployment
      // remove the old module and create a new OSGiModule
      if (metaData instanceof OSGiClassLoadingMetaData && unit.isAttachmentPresent(Module.class))
      {
         Module module = unit.removeAttachment(Module.class);
         ClassLoading classLoading = getClassLoading();
         classLoading.removeModule(module);
         unit.removeAttachment(Module.class);
         deployInternal(unit, metaData);
      }
   }
}
