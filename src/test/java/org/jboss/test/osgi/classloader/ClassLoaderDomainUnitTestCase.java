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
package org.jboss.test.osgi.classloader;

// $Id$

import static org.junit.Assert.fail;

import java.net.URL;

import org.jboss.classloader.plugins.jdk.AbstractJDKChecker;
import org.jboss.classloader.plugins.system.DefaultClassLoaderSystem;
import org.jboss.classloader.spi.ClassLoaderDomain;
import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.classloader.spi.ParentPolicy;
import org.jboss.classloader.spi.filter.ClassFilter;
import org.jboss.classloader.spi.filter.PackageClassFilter;
import org.jboss.classloading.spi.vfs.policy.VFSClassLoaderPolicy;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.testing.OSGiTestHelper;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * ClassLoaderDomainUnitTestCase.
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 03-Sep-2009
 */
public class ClassLoaderDomainUnitTestCase
{
   private static final String OSGI_CLASSLOADER_DOMAIN = "OSGiDomain";
   private ClassLoaderSystem classLoaderSystem;

   @Before
   public void setUp()
   {
      classLoaderSystem = new DefaultClassLoaderSystem();
      AbstractJDKChecker.getExcluded().add(getClass());
   }

   @Test
   public void testSystemPolicy() throws Exception
   {
      ClassLoaderDomain domain = new ClassLoaderDomain(OSGI_CLASSLOADER_DOMAIN);
      classLoaderSystem.registerDomain(domain);
      
      // Setup the class filter
      String filteredPackages = Logger.class.getPackage().getName();
      PackageClassFilter classFilter = PackageClassFilter.createPackageClassFilterFromString(filteredPackages);
      classFilter.setIncludeJava(true);

      domain.setParentPolicy(new ParentPolicy(classFilter, ClassFilter.NOTHING));

      URL coreURL = new OSGiTestHelper().getTestArchiveURL("bundles/org.osgi.core.jar");
      VirtualFile coreVF = VFS.createNewRoot(coreURL);

      ClassLoaderPolicy systemPolicy = new VFSClassLoaderPolicy("OSGiSystemPolicy", new VirtualFile[] { coreVF });
      ClassLoader classLoader = classLoaderSystem.registerClassLoaderPolicy(OSGI_CLASSLOADER_DOMAIN, systemPolicy);

      // Load JDK class
      assertLoadClass(classLoader, String.class.getName(), true);
      
      // Load from org.osgi.core
      assertLoadClass(classLoader, Bundle.class.getName(), true);
      
      // Load from system classpath 
      assertLoadClass(classLoader, Logger.class.getName(), true);
      
      // No access to implementation
      assertLoadClass(classLoader, OSGiBundleManager.class.getName(), false);
   }
   
   private void assertLoadClass(ClassLoader classLoader, String name, boolean success)
   {
      try
      {
         classLoader.loadClass(name);
         if (success == false)
            fail("Expected ClassNotFoundException for '" + name + "' from " + classLoader);
      }
      catch (ClassNotFoundException ex)
      {
         if (success)
            fail("Cannot load '" + name + "' from " + classLoader);
      }
   }
}
