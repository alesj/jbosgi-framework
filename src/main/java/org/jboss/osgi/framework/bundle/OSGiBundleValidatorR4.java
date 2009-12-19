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
package org.jboss.osgi.framework.bundle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.PackageAttribute;
import org.osgi.framework.Constants;

/**
 * OSGiBundleManager.
 * 
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class OSGiBundleValidatorR4 implements OSGiBundleValidator
{
   private OSGiBundleManager bundleManager;
   
   public OSGiBundleValidatorR4(OSGiBundleManager bundleManager)
   {
      this.bundleManager = bundleManager;
   }


   /**
    * Validate the bundle
    * 
    * @param bundleState the bundle state
    */
   @SuppressWarnings("deprecation")
   public void validateBundle(AbstractBundleState bundleState)
   {
      OSGiMetaData metaData = bundleState.getMetaData();
      
      // Missing Bundle-SymbolicName
      String symbolicName = bundleState.getSymbolicName();
      if (symbolicName == null)
         throw new IllegalStateException("Missing Bundle-SymbolicName in: " + bundleState);
      
      // Bundle-ManifestVersion value not equal to 2, unless the Framework specifically recognizes the semantics of a later release.
      int manifestVersion = metaData.getBundleManifestVersion();
      if (manifestVersion > 2)
         throw new IllegalStateException("Unsupported manifest version " + manifestVersion + " for " + bundleState);

      // [TODO] Duplicate attribute or duplicate directive (except in the Bundle-Native code clause).
      
      // Multiple imports of a given package.
      List<PackageAttribute> importPackages = metaData.getImportPackages();
      if (importPackages != null && importPackages.isEmpty() == false)
      {
         Set<String> packages = new HashSet<String>();
         for (PackageAttribute packageAttribute : importPackages)
         {
            String packageName = packageAttribute.getAttribute();
            if (packages.contains(packageName))
               throw new IllegalStateException("Duplicate import of package " + packageName + " for " + bundleState);
            packages.add(packageName);

            if (packageName.startsWith("java."))
               throw new IllegalStateException("Not allowed to import java.* for " + bundleState);

            String version = packageAttribute.getAttributeValue(Constants.VERSION_ATTRIBUTE, String.class);
            String specificationVersion = packageAttribute.getAttributeValue(Constants.PACKAGE_SPECIFICATION_VERSION, String.class);
            if (version != null && specificationVersion != null && version.equals(specificationVersion) == false)
               throw new IllegalStateException(packageName + " version and specification version should be the same for " + bundleState);
         }
      }
      
      // Export or import of java.*.
      List<PackageAttribute> exportPackages = metaData.getExportPackages();
      if (exportPackages != null && exportPackages.isEmpty() == false)
      {
         for (PackageAttribute packageAttribute : exportPackages)
         {
            String packageName = packageAttribute.getAttribute();
            if (packageName.startsWith("java."))
               throw new IllegalStateException("Not allowed to export java.* for " + bundleState);
         }
      }
      
      // [TODO] Export-Package with a mandatory attribute that is not defined.
      
      // Installing a bundle that has the same symbolic name and version as an already installed bundle.
      for (AbstractBundleState bundle : bundleManager.getBundles())
      {
         OSGiMetaData other = bundle.getMetaData();
         if (symbolicName.equals(other.getBundleSymbolicName()))
         {
            if (other.isSingleton() && metaData.isSingleton())
               throw new IllegalStateException("Cannot install singleton " + bundleState + " another singleton is already installed: " + bundle.getLocation());
            if (other.getBundleVersion().equals(metaData.getBundleVersion()))
               throw new IllegalStateException("Cannot install " + bundleState + " a bundle with that name and version is already installed: "
                     + bundle.getLocation());
         }
      }
      
      // [TODO] Updating a bundle to a bundle that has the same symbolic name and version as another installed bundle.
      
      // [TODO] Any syntactic error (for example, improperly formatted version or bundle symbolic name, unrecognized directive value, etc.).
      
      // [TODO] Specification-version and version specified together (for the same package(s)) but with different values.
      
      // [TODO] The manifest lists a OSGI-INF/permission.perm file but no such file is present.
      
      // [TODO] Requiring the same bundle symbolic name more than once
   }
}
