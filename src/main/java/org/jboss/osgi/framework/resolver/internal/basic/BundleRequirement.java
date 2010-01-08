/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.osgi.framework.resolver.internal.basic;

import org.jboss.classloading.plugins.metadata.PackageRequirement;
import org.jboss.osgi.framework.bundle.AbstractDeployedBundleState;

/**
 * An association of bundle/requirement.
 * 
 * @author thomas.diesler@jboss.com
 * @since 10-Sep-2009
 */
class BundleRequirement
{
   private AbstractDeployedBundleState bundle;
   private PackageRequirement packageRequirement;
   private BundleCapability wire;
   
   BundleRequirement(AbstractDeployedBundleState bundle, PackageRequirement packageRequirement)
   {
      if (bundle == null)
         throw new IllegalArgumentException("Null bundle");
      if (packageRequirement == null)
         throw new IllegalArgumentException("Null packageRequirement");
      
      this.bundle = bundle;
      this.packageRequirement = packageRequirement;
   }
   
   BundleCapability getWiredCapability()
   {
      return wire;
   }

   AbstractDeployedBundleState getImportingBundle()
   {
      return bundle;
   }

   PackageRequirement getPackageRequirement()
   {
      return packageRequirement;
   }

   void wireCapability(BundleCapability bundleCapability)
   {
      wire = bundleCapability;
      if (bundleCapability != null)
         bundleCapability.wireRequirement(this);
   }

   void unwireCapability()
   {
      if (wire != null)
      {
         wire.unwireRequirement(this);
         wire = null;
      }
   }
   
   @Override
   public boolean equals(Object obj)
   {
      if (obj instanceof BundleRequirement == false)
         return false;
      if (obj == this)
         return true;
      return toString().equals(obj.toString());
   }

   @Override
   public int hashCode()
   {
      return toString().hashCode();
   }

   @Override
   public String toString()
   {
      String name = packageRequirement.getName();
      return "BundleRequirement[" + name + "," + bundle + "]";
   }
}