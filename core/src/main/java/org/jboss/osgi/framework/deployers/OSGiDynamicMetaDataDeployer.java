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
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.internal.DynamicOSGiMetaData;
import org.jboss.osgi.framework.metadata.internal.OSGiManifestMetaData;
import org.osgi.framework.Constants;

/**
 * A deployer that creates {@link OSGiMetaData} for deployments that are not real bundles.
 * 
 * A real bundle is expected to have valid OSGi manifest entries. In which case, the {@link OSGiManifestParsingDeployer}
 * would already have attached the {@link OSGiMetaData}.
 * 
 * @author thomas.diesler@jboss.com
 * @since 04-Jun-2010
 */
public class OSGiDynamicMetaDataDeployer extends AbstractDeployer
{
   public OSGiDynamicMetaDataDeployer()
   {
      setStage(DeploymentStages.POST_PARSE);
      setOutput(OSGiMetaData.class);
      setTopLevelOnly(true);
   }

   @Override
   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      if (unit.isAttachmentPresent(OSGiMetaData.class))
         return;
      
      // The {@link OSGiManifestMetaData} is likely to have been created by the {@link OSGiManifestParsingDeployer}
      // This is the {@link OSGiMetaData} with the higest priority, in which case we don't create one. 
      if (unit.isAttachmentPresent(OSGiManifestMetaData.class))
      {
         OSGiMetaData metadata = unit.getAttachment(OSGiManifestMetaData.class);
         unit.addAttachment(OSGiMetaData.class, metadata);
         return;
      }
      
      if (unit.isAttachmentPresent(BeanMetaData.class))
      {
         BeanMetaData bmd = unit.getAttachment(BeanMetaData.class);
         OSGiMetaData metadata = processBeanMetaData(unit, bmd);
         unit.addAttachment(OSGiMetaData.class, metadata);
      }
   }

   private OSGiMetaData processBeanMetaData(DeploymentUnit unit, BeanMetaData bmd)
   {
      String symbolicName = unit.getName();
      DynamicOSGiMetaData metadata = new DynamicOSGiMetaData(symbolicName);
      
      // Add an Export-Package definition from the bean's package
      String className = bmd.getBean();
      String packageName = className.substring(0, className.lastIndexOf("."));
      metadata.addMainAttribute(Constants.EXPORT_PACKAGE, packageName);
      
      // [TODO] Read the manifest and a version attribute if available
      System.out.println(bmd);
      
      return metadata;
   }
}
