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

import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;

import org.jboss.deployers.vfs.spi.deployer.ManifestDeployer;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.internal.AbstractOSGiMetaData;
import org.jboss.osgi.spi.OSGiConstants;
import org.jboss.virtual.VirtualFile;
import org.osgi.framework.Constants;

/**
 * OSGiManifestParsingDeployer.<p>
 * 
 * This deployer attaches OSGiMetaData to the deployment if it is a real OSGi bundle.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class OSGiManifestParsingDeployer extends ManifestDeployer<OSGiMetaData>
{
   /**
    * Create a new OSGiManifestParsingDeployer.
    */
   public OSGiManifestParsingDeployer()
   {
      super(OSGiMetaData.class);
      setTopLevelOnly(true);
   }
   
   @Override
   protected OSGiMetaData createMetaData(Manifest manifest) throws Exception
   {
      // To be a true OSGi bundle it must have a bundle symbolic name in the manfiest
      if (manifest.getMainAttributes().get(new Name(Constants.BUNDLE_SYMBOLICNAME)) == null)
         return null;

      return new AbstractOSGiMetaData(manifest);
   }

   @Override
   protected void init(VFSDeploymentUnit unit, OSGiMetaData metaData, VirtualFile file) throws Exception
   {
      super.init(unit, metaData, file);

      String symbolicName = metaData.getBundleSymbolicName();
      log.debug("Bundle-SymbolicName: " + symbolicName + " in " + file);

      // Add a marker that this is an OSGi deployment
      unit.addAttachment(OSGiConstants.KEY_BUNDLE_SYMBOLIC_NAME, symbolicName);
   }
}
