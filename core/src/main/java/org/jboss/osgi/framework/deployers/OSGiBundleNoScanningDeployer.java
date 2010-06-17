/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

import java.util.List;

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.scanning.spi.metadata.PathMetaData;
import org.jboss.scanning.spi.metadata.ScanningMetaData;

/**
 * Prevent scanning of OSGi bundles.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 17-Jun-2010
 */
public class OSGiBundleNoScanningDeployer extends AbstractDeployer
{
   public OSGiBundleNoScanningDeployer()
   {
      setStage(DeploymentStages.CLASSLOADER);
      addInput(ScanningMetaData.class);
      addInput(OSGiMetaData.class);
   }

   @Override
   public void deploy(DeploymentUnit unit) throws DeploymentException
   {
      if (unit.isAttachmentPresent(ScanningMetaData.class) || unit.isAttachmentPresent(OSGiMetaData.class) == false)
         return;
      
      ScanningMetaData smd = new ScanningMetaData()
      {
         @Override
         public List<PathMetaData> getPaths()
         {
            return null;
         }
      };

      unit.addAttachment(ScanningMetaData.class, smd);
   }
}