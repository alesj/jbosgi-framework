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
package org.jboss.osgi.framework.resolver.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.classloading.plugins.metadata.PackageRequirement;
import org.jboss.osgi.framework.bundle.DeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.classloading.OSGiRequirement;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.plugins.internal.AbstractPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * An abstract resolver that maintains the {@link ResolverBundle} bundles.
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public abstract class AbstractResolver extends AbstractPlugin implements ResolverPlugin
{
   private Map<OSGiBundleState, ResolverBundle> resolverBundleMap = new ConcurrentHashMap<OSGiBundleState, ResolverBundle>();

   public AbstractResolver(OSGiBundleManager bundleManager)
   {
      super(bundleManager);
   }

   public List<ResolverBundle> getBundles()
   {
      List<ResolverBundle> values = new ArrayList<ResolverBundle>(resolverBundleMap.values());
      return Collections.unmodifiableList(values);
   }

   public ResolverBundle getBundle(Bundle bundle)
   {
      if (bundle == null)
         throw new IllegalArgumentException("Null bundle");
      
      OSGiBundleState bundleState = OSGiBundleState.assertBundleState(bundle);
      ResolverBundle resolverBundle = resolverBundleMap.get(bundleState);
      if (resolverBundle == null)
         throw new IllegalStateException("Cannot obtain resolver bundle for: " + bundleState);
      
      return resolverBundle;
   }

   public ResolverBundle getBundle(String symbolicName, Version version)
   {
      if (symbolicName == null)
         throw new IllegalArgumentException("Null symbolicName");

      if (version == null)
         version = Version.emptyVersion;
      
      ResolverBundle retBundle = null;
      for (ResolverBundle aux : resolverBundleMap.values())
      {
         if (aux.getSymbolicName().equals(symbolicName) && aux.getVersion().equals(version))
         {
            retBundle = aux;
            break;
         }
      }
      return retBundle;
   }

   public void addBundle(Bundle bundle)
   {
      OSGiBundleState bundleState = OSGiBundleState.assertBundleState(bundle);
      ResolverBundleImpl resBundle = new ResolverBundleImpl(bundle);
      resolverBundleMap.put(bundleState, resBundle);
   }

   public void removeBundle(Bundle bundle)
   {
      DeployedBundleState bundleState = OSGiBundleState.assertBundleState(bundle);
      resolverBundleMap.remove(bundleState);
   }

   public boolean match(Bundle importer, Bundle exporter, OSGiRequirement osgireq)
   {
      if (importer == null)
         throw new IllegalArgumentException("Null importer");
      if (exporter == null)
         throw new IllegalArgumentException("Null exporter");
      
      if (osgireq instanceof PackageRequirement == false)
         return false;
      
      String packageName = ((PackageRequirement)osgireq).getName();
      if (packageName == null)
         throw new IllegalArgumentException("Null packageName");
      
      ExportPackage exportPackage = getExporter(importer, packageName);
      if (exportPackage == null)
         return false;
      
      Bundle packageOwner = exportPackage.getOwner().getBundle();
      boolean match = packageOwner.getSymbolicName().equals(exporter.getSymbolicName());
      match = match && packageOwner.getVersion().equals(exporter.getVersion());
      
      return match;
   }

   /**
    * Get the exporter for the given import package.
    * 
    * @param importer The bundle that imports the package.
    * @param packageName The import package name
    * @return The export package that the import is wired to or null if the import is not yet resolved.
    */
   public abstract ExportPackage getExporter(Bundle importer, String packageName);
}