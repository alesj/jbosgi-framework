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

import java.io.IOException;
import java.util.List;

import org.jboss.classloader.plugins.filter.CombiningClassFilter;
import org.jboss.classloader.plugins.jdk.AbstractJDKChecker;
import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloader.spi.ParentPolicy;
import org.jboss.classloader.spi.base.BaseClassLoader;
import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloader.spi.filter.ClassFilterUtils;
import org.jboss.classloader.spi.filter.PackageClassFilter;
import org.jboss.classloader.spi.filter.RecursivePackageClassFilter;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.plugins.SystemPackagesPlugin;

/**
 * The OSGi ClassLoaderSystem.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author thomas.diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class OSGiClassLoaderSystem extends ClassLoaderSystem
{
   private OSGiBundleManager bundleManager;
   
   public OSGiClassLoaderSystem()
   {
      AbstractJDKChecker.getExcluded().add(AbstractBundleState.class);
      AbstractJDKChecker.getExcluded().add(OSGiBundleState.class);
      
      ClassFilter javaFilter = RecursivePackageClassFilter.createRecursivePackageClassFilter("java");
      ClassFilter filter = CombiningClassFilter.create(javaFilter, OSGiCoreClassFilter.INSTANCE);
      
      ClassLoaderDomain defaultDomain = getDefaultDomain();
      defaultDomain.setParentPolicy(new ParentPolicy(filter, ClassFilterUtils.NOTHING));
   }

   public void setBundleManager(OSGiBundleManager bundleManager)
   {
      this.bundleManager = bundleManager;
   }
   
   public void start() throws IOException
   {
      if (bundleManager == null)
         throw new IllegalArgumentException("Null bundleManager");

      // Initialize the configured system packages
      ClassFilter javaFilter = RecursivePackageClassFilter.createRecursivePackageClassFilter("java");
      ClassFilter systemFilter = PackageClassFilter.createPackageClassFilterFromString(getSystemPackagesAsString());
      ClassFilter filter = CombiningClassFilter.create(javaFilter, OSGiCoreClassFilter.INSTANCE, systemFilter);

      // Setup the domain's parent policy
      ClassLoaderDomain defaultDomain = getDefaultDomain();
      defaultDomain.setParentPolicy(new ParentPolicy(filter, ClassFilterUtils.NOTHING));
      
      // Make the default domain available to the bundle manager
      bundleManager.setClassLoaderDomain(defaultDomain);
   }

   @Override
   protected ClassLoaderDomain createDomain(String name)
   {
      return new ClassLoaderDomain(name);
   }

   @Override
   protected BaseClassLoader createClassLoader(ClassLoaderPolicy policy)
   {
      BaseClassLoader classLoader;
      if (policy instanceof OSGiClassLoaderPolicy)
         classLoader = new OSGiBundleClassLoader(policy);
      else
         classLoader = super.createClassLoader(policy);

      return classLoader;
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