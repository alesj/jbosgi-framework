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

import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.kernel.spi.deployment.KernelDeployment;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.OSGiMetaDataBuilder;

/**
 * A deployer that creates {@link OSGiMetaData} from an attached {@link KernelDeployment}.
 * 
 * @author thomas.diesler@jboss.com
 * @since 04-Jun-2010
 */
public class OSGiKernelDeploymentDeployer extends AbstractOSGiMetaDataDeployer<KernelDeployment>
{
   public OSGiKernelDeploymentDeployer()
   {
      super(KernelDeployment.class);
   }

   @Override
   protected OSGiMetaData deployInternal(DeploymentUnit unit, KernelDeployment attachment)
   {
      String symbolicName = unit.getName();
      OSGiMetaDataBuilder builder = OSGiMetaDataBuilder.createBuilder(symbolicName);
      
      // Add an Export-Package definition from the bean's package
      for (BeanMetaData bmd : attachment.getBeans())
      {
         String className = bmd.getBean();
         String packageName = className.substring(0, className.lastIndexOf("."));
         builder.addExportPackages(packageName);
      }
      
      // [TODO] Read the manifest and a version attribute if available
      
      return builder.getOSGiMetaData();
   }
}
