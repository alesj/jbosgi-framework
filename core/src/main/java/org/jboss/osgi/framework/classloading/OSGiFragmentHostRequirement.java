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
package org.jboss.osgi.framework.classloading;

import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.classloading.OSGiClassLoadingMetaData.FragmentHostMetaData;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;

/**
 * A ModuleRequirement that is associated with Fragment-Host.
 * 
 * @author thomas.diesler@jboss.com
 * @since 03-Jun-2010
 */
public class OSGiFragmentHostRequirement extends OSGiBundleRequirement
{
   private static final long serialVersionUID = -1337312549822378204L;

   public static OSGiFragmentHostRequirement create(AbstractBundleState bundleState, FragmentHostMetaData hostMetaData)
   {
      if (hostMetaData == null)
         throw new IllegalArgumentException("Null host metadata");

      String name = hostMetaData.getSymbolicName();
      VersionRange versionRange = hostMetaData.getVersionRange();
      ParameterizedAttribute metadata = hostMetaData.getMetadata();

      return new OSGiFragmentHostRequirement(bundleState, name, versionRange, metadata);
   }

   private OSGiFragmentHostRequirement(AbstractBundleState bundleState, String name, VersionRange versionRange, ParameterizedAttribute metadata)
   {
      super(bundleState, name, versionRange, metadata);
   }
}
