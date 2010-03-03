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
package org.jboss.osgi.framework.resolver.internal;

import java.util.List;

import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.PackageAttribute;
import org.jboss.osgi.framework.metadata.ParameterizedAttribute;
import org.osgi.framework.Bundle;

/**
 * A ResolverBundle implementation.
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public class ResolverBundleImpl extends AbstractResolverBundle
{
   private OSGiMetaData osgiMetaData;

   public ResolverBundleImpl(Bundle bundle)
   {
      super(bundle);
      
      this.osgiMetaData = OSGiBundleState.assertBundleState(bundle).getOSGiMetaData();

      // Initialize exported packages
      List<PackageAttribute> exportPackages = osgiMetaData.getExportPackages();
      if (exportPackages != null)
      {
         for (PackageAttribute attr : exportPackages)
         {
            String packageName = attr.getAttribute();
            exportedPackages.put(packageName, new ExportPackageImpl(this, attr));
         }
      }

      // Initialize imported packages
      List<PackageAttribute> importPackages = osgiMetaData.getImportPackages();
      if (importPackages != null)
      {
         for (PackageAttribute attr : importPackages)
         {
            String packageName = attr.getAttribute();
            importedPackages.put(packageName, new ImportPackageImpl(this, attr));
         }
      }

      // Initialize required bundles
      List<ParameterizedAttribute> requireBundles = osgiMetaData.getRequireBundles();
      if (requireBundles != null)
      {
         for (ParameterizedAttribute attr : requireBundles)
         {
            String symbolicName = attr.getAttribute();
            RequiredBundleImpl req = new RequiredBundleImpl(this, attr);
            requiredBundles.put(symbolicName, req);
         }
      }
   }

   public boolean isSingleton()
   {
      return osgiMetaData.isSingleton();
   }
}