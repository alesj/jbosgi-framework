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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.classloading.plugins.metadata.PackageCapability;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractDeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.classloading.OSGiPackageCapability;
import org.jboss.osgi.framework.classloading.OSGiPackageRequirement;

/**
 * An association of bundle/capability.
 * 
 * @author thomas.diesler@jboss.com
 * @since 10-Sep-2009
 */
class BundleCapability
{
   private AbstractDeployedBundleState bundle;
   private PackageCapability packageCapability;
   private List<BundleRequirement> wires;
   
   BundleCapability(AbstractDeployedBundleState bundle, PackageCapability packageCapability)
   {
      if (bundle == null)
         throw new IllegalArgumentException("Null bundle");
      if (packageCapability == null)
         throw new IllegalArgumentException("Null packageCapability");
      
      this.bundle = bundle;
      this.packageCapability = packageCapability;
   }

   AbstractDeployedBundleState getExportingBundle()
   {
      return bundle;
   }

   Module getExportingModule()
   {
      AbstractDeployedBundleState bundleState = OSGiBundleState.assertBundleState(bundle);
      DeploymentUnit unit = bundleState.getDeploymentUnit();
      return unit.getAttachment(Module.class);
   }
   
   PackageCapability getPackageCapability()
   {
      return packageCapability;
   }

   List<BundleRequirement> getWiredRequirements()
   {
      if (wires == null)
         return Collections.emptyList();
      
      return Collections.unmodifiableList(wires);
   }

   boolean matches(BundleRequirement bundleRequirement)
   {
      OSGiPackageCapability osgiPackageCapability = (OSGiPackageCapability)packageCapability;
      OSGiPackageRequirement osgiPackageRequirement = (OSGiPackageRequirement)bundleRequirement.getPackageRequirement();
      return osgiPackageCapability.matchPackageAttributes(osgiPackageRequirement);
   }

   void wireRequirement(BundleRequirement bundleRequirement)
   {
      if (wires == null)
         wires = new CopyOnWriteArrayList<BundleRequirement>();
      
      wires.add(bundleRequirement);
   }
   
   void unwireRequirement(BundleRequirement bundleRequirement)
   {
      if (wires != null)
         wires.remove(bundleRequirement);
   }
   
   @Override
   public boolean equals(Object obj)
   {
      if (obj instanceof BundleCapability == false)
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
      String name = packageCapability.getName();
      return "BundleCapability[" + name + "," + bundle + "]";
   }
}