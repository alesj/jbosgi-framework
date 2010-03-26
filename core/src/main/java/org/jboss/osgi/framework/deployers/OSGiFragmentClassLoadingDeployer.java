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
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.classloading.OSGiClassLoadingMetaData;
import org.jboss.osgi.framework.classloading.OSGiClassLoadingMetaData.FragmentHostMetaData;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.Parameter;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

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
   public void deploy(DeploymentUnit unit, OSGiMetaData osgiMetaData) throws DeploymentException
   {
      super.deploy(unit, osgiMetaData);

      // Return if this is not a bundle fragment 
      AbstractBundleState bundleState = unit.getAttachment(AbstractBundleState.class);
      if (bundleState.isFragment() == false)
         return;

      OSGiClassLoadingMetaData classLoadingMetaData = (OSGiClassLoadingMetaData)unit.getAttachment(ClassLoadingMetaData.class);

      // Initialize the Fragment-Host 
      ParameterizedAttribute hostAttr = osgiMetaData.getFragmentHost();
      FragmentHostMetaData fragmentHost = new FragmentHostMetaData(hostAttr.getAttribute());
      classLoadingMetaData.setFragmentHost(fragmentHost);

      Parameter bundleVersionAttr = hostAttr.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
      if (bundleVersionAttr != null)
         fragmentHost.setBundleVersion((Version)bundleVersionAttr.getValue());

      Parameter extensionDirective = hostAttr.getDirective(Constants.EXTENSION_DIRECTIVE);
      if (extensionDirective != null)
         fragmentHost.setExtension((String)extensionDirective.getValue());

      // TODO Modify the CL metadata of the host such that eventually the CL policy
      // contains a DelegateLoader for the attached fragment

      // Adding the fragment as an OSGiBundleRequirement to the host does not work because 
      // those requirements end up as DependencyItems already during the INSTALL phase. 
      // Remember to do equivalent code in OSGiBundleClassLoadingDeployer
      // in case the fragment gets installed before the host.
   }
}
