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

import org.jboss.classloading.plugins.metadata.ModuleRequirement;
import org.jboss.classloading.spi.version.VersionRange;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.classloading.OSGiClassLoadingMetaData.FragmentHostMetaData;

/**
 * A ModuleRequirement that is associated with Fragment-Host.
 * 
 * @author thomas.diesler@jboss.com
 * @since 03-Jun-2010
 */
public class OSGiFragmentHostRequirement extends ModuleRequirement implements OSGiRequirement
{
   private static final long serialVersionUID = -1337312549822378204L;

   private AbstractBundleState bundleState;

   public static OSGiFragmentHostRequirement create(AbstractBundleState bundleState, FragmentHostMetaData metadata)
   {
      if (bundleState == null)
         throw new IllegalArgumentException("Null bundleState");
      if (metadata == null)
         throw new IllegalArgumentException("Null metadata");

      String name = metadata.getSymbolicName();
      VersionRange versionRange = metadata.getVersionRange();

      return new OSGiFragmentHostRequirement(bundleState, name, versionRange);
   }

   private OSGiFragmentHostRequirement(AbstractBundleState bundleState, String name, VersionRange versionRange)
   {
      super(name, versionRange);
      this.bundleState = bundleState;
   }

   @Override
   public AbstractBundleState getBundleState()
   {
      return bundleState;
   }
}
