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

// $Id$

import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.classloading.spi.metadata.RequirementsMetaData;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.classloading.OSGiClassLoadingMetaData;
import org.jboss.osgi.framework.classloading.OSGiClassLoadingMetaData.FragmentHostMetaData;
import org.jboss.osgi.framework.classloading.OSGiFragmentHostRequirement;
import org.jboss.osgi.resolver.XFragmentHostRequirement;
import org.jboss.osgi.resolver.XModule;

/**
 * An OSGi classloading deployer, that maps osgi metadata into classloading metadata
 * for fragment bundles.
 * 
 * @author thomas.diesler@jboss.com
 * @since 07-Jan-2010
 */
public class OSGiFragmentClassLoadingDeployer extends AbstractClassLoadingDeployer
{
   @Override
   public void deploy(DeploymentUnit unit, XModule resolverModule) throws DeploymentException
   {
      super.deploy(unit, resolverModule);

      // Return if this is not a bundle fragment 
      AbstractBundleState bundleState = unit.getAttachment(AbstractBundleState.class);
      if (resolverModule.isFragment() == true)
      {
         OSGiClassLoadingMetaData classLoadingMetaData = (OSGiClassLoadingMetaData)unit.getAttachment(ClassLoadingMetaData.class);

         // Initialize the Fragment-Host 
         XFragmentHostRequirement hostReq = resolverModule.getHostRequirement();
         FragmentHostMetaData hostMetaData = new FragmentHostMetaData(hostReq);
         classLoadingMetaData.setFragmentHost(hostMetaData);

         // Add the fragment host requirement
         RequirementsMetaData requirements = classLoadingMetaData.getRequirements();
         requirements.addRequirement(OSGiFragmentHostRequirement.create(bundleState, hostMetaData));
      }
   }
}
