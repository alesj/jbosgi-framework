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

import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.internal.OSGiManifestMetaData;

/**
 * A deployer that creates {@link OSGiMetaData} from an attached {@link OSGiManifestMetaData}.
 * 
 * @author thomas.diesler@jboss.com
 * @since 04-Jun-2010
 */
public class OSGiManifestMetaDataDeployer extends AbstractOSGiMetaDataDeployer<OSGiManifestMetaData>
{
   public OSGiManifestMetaDataDeployer()
   {
      super(OSGiManifestMetaData.class);
   }

   @Override
   protected OSGiMetaData createOSGiMetaData(DeploymentUnit unit, OSGiManifestMetaData attachment)
   {
      // The {@link OSGiManifestMetaData} is likely to have been created by the {@link OSGiManifestParsingDeployer}
      // This is the {@link OSGiMetaData} with the higest priority. 
      return attachment;
   }
}
