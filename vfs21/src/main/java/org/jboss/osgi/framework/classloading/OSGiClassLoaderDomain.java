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

// $Id: $

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.classloader.plugins.filter.CombiningClassFilter;
import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloader.spi.ParentPolicy;
import org.jboss.classloader.spi.base.BaseClassLoader;
import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloader.spi.filter.ClassFilterUtils;
import org.jboss.classloader.spi.filter.PackageClassFilter;
import org.jboss.classloader.spi.filter.RecursivePackageClassFilter;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiSystemState;
import org.jboss.osgi.framework.plugins.SystemPackagesPlugin;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;

/**
 * OSGiClassLoaderDomain.<p>
 * 
 * @author thomas.diesler@jboss.com
 * @since 03-Sep-2009
 */
public class OSGiClassLoaderDomain extends ClassLoaderDomain
{
   private ClassLoaderSystem classLoaderSystem;
   private OSGiBundleManager bundleManager;
   private List<URL> classPath = new ArrayList<URL>();

   /**
    * Create a new OSGiClassLoaderDomain.
    * @param domainName the domain name
    * @throws IllegalArgumentException for a null bundle manager
    */
   public OSGiClassLoaderDomain(String domainName)
   {
      super(domainName);
   }

   public void setClassLoaderSystem(ClassLoaderSystem classLoaderSystem)
   {
      this.classLoaderSystem = classLoaderSystem;
   }

   public void setBundleManager(OSGiBundleManager bundleManager)
   {
      this.bundleManager = bundleManager;
   }

   public void setClassPath(List<URL> classPath)
   {
      this.classPath = classPath;
   }

   @Override
   protected Class<?> loadClass(BaseClassLoader classLoader, String name, boolean allExports) throws ClassNotFoundException
   {
      return super.loadClass(classLoader, name, allExports);
   }

   public void start() throws IOException
   {
      if (classLoaderSystem == null)
         throw new IllegalArgumentException("Null classLoaderSystem");
      if (bundleManager == null)
         throw new IllegalArgumentException("Null bundleManager");
      if (classPath == null)
         throw new IllegalArgumentException("Null classPath");

      // Register the domain with the ClassLoaderSystem
      classLoaderSystem.registerDomain(this);

      // Initialize the configured system packages
      ClassFilter systemFilter = PackageClassFilter.createPackageClassFilterFromString(getSystemPackagesAsString());
      ClassFilter javaFilter = RecursivePackageClassFilter.createRecursivePackageClassFilter("java");
      ClassFilter filter = CombiningClassFilter.create(javaFilter, OSGiCoreClassFilter.INSTANCE, systemFilter);

      // Setup the domain's parent policy
      setParentPolicy(new ParentPolicy(filter, ClassFilterUtils.NOTHING));

      // Initialize the configured policy roots
      VirtualFile[] roots = new VirtualFile[classPath.size()];
      for (int i = 0; i < classPath.size(); i++)
         roots[i] = VFS.createNewRoot(classPath.get(i));

      // Create and register the ClassLoaderPolicy
      OSGiSystemState systemBundle = bundleManager.getSystemBundle();
      ClassLoaderPolicy systemPolicy = new OSGiClassLoaderPolicy(systemBundle, roots);
      classLoaderSystem.registerClassLoaderPolicy(getName(), systemPolicy);
   }

   private String getSystemPackagesAsString()
   {
      SystemPackagesPlugin syspackPlugin = bundleManager.getPlugin(SystemPackagesPlugin.class);
      List<String> sysPackages = syspackPlugin.getSystemPackages(false);
      StringBuffer sysPackageString = new StringBuffer();
      for (String name : sysPackages)
         sysPackageString.append(name + ",");
      
      return sysPackageString.toString();
   }
}