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
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.internal.OSGiManifestMetaData;

/**
 * A deployer that creates {@link OSGiMetaData} for deployments that are not real bundles.
 * 
 * A real bundle is expected to have valid OSGi manifest entries. In which case, the {@link OSGiManifestParsingDeployer}
 * would already have attached the {@link OSGiMetaData}.
 * 
 * @author thomas.diesler@jboss.com
 * @since 04-Jun-2010
 */
public abstract class AbstractOSGiMetaDataDeployer<T> extends AbstractDeployer
{
   // The optional metadata attachment
   private Class<T> attachmentType;
   
   protected AbstractOSGiMetaDataDeployer(Class<T> attachmentType)
   {
      if (attachmentType == null)
         throw new IllegalArgumentException("Null attachment type");
      this.attachmentType = attachmentType;
      
      setStage(DeploymentStages.POST_PARSE);
      setOutput(OSGiMetaData.class);
      setInput(attachmentType);
      setTopLevelOnly(true);
   }

   @Override
   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      if (unit.isAttachmentPresent(OSGiMetaData.class))
         return;
      
      // The {@link OSGiManifestMetaData} is likely to have been created by the {@link OSGiManifestParsingDeployer}
      // This is the {@link OSGiMetaData} with the higest priority, in which case we don't look further.
      if (unit.isAttachmentPresent(OSGiManifestMetaData.class))
      {
         OSGiMetaData metadata = unit.getAttachment(OSGiManifestMetaData.class);
         unit.addAttachment(OSGiMetaData.class, metadata);
         return;
      }
      
      // Process the given metadata type and turn it into an instance of {@link OSGiMetaData}
      if (unit.isAttachmentPresent(attachmentType))
      {
         T attachment = unit.getAttachment(attachmentType);
         OSGiMetaData metadata = deployInternal(unit, attachment);
         unit.addAttachment(OSGiMetaData.class, metadata);
      }
   }

   /**
    * Overwrite to generate an instance of {@link OSGiMetaData} from the given attachment. 
    */
   protected abstract OSGiMetaData deployInternal(DeploymentUnit unit, T attachment);
}
