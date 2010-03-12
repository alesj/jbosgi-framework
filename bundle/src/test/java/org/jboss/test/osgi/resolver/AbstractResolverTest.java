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
package org.jboss.test.osgi.resolver;

// $Id$

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.launch.OSGiFramework;
import org.jboss.osgi.framework.plugins.PackageAdminPlugin;
import org.jboss.osgi.framework.plugins.Plugin;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.resolver.Resolver;
import org.jboss.osgi.spi.framework.OSGiBootstrap;
import org.jboss.osgi.spi.framework.OSGiBootstrapProvider;
import org.jboss.osgi.testing.OSGiRuntimeTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * An abstract {@link Resolver} test.
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public abstract class AbstractResolverTest extends OSGiRuntimeTest
{
   protected static OSGiFramework framework;

   @BeforeClass
   public static void beforeClass() throws BundleException
   {
      OSGiBootstrapProvider bootProvider = OSGiBootstrap.getBootstrapProvider();
      framework = (OSGiFramework)bootProvider.getFramework();
      framework.start();
   }

   @AfterClass
   public static void afterClass() throws BundleException
   {
      if (framework != null)
      {
         framework.stop();
         framework = null;
      }
   }

   @Before
   public void setUp() throws Exception
   {
      Resolver installedResolver = getInstalledResolver();
      Resolver testResolver = getTestResolver();
      if (installedResolver != testResolver)
      {
         OSGiBundleManager bundleManager = framework.getBundleManager();
         if (installedResolver != null)
            bundleManager.removePlugin((Plugin)installedResolver);
         if (testResolver != null)
            bundleManager.addPlugin((Plugin)testResolver);
      }
   }

   // Overwrite to provide a different resolver
   protected Resolver getTestResolver()
   {
      return getInstalledResolver();
   }

   protected Resolver getInstalledResolver()
   {
      OSGiBundleManager bundleManager = framework.getBundleManager();
      return bundleManager.getOptionalPlugin(ResolverPlugin.class);
   }

   protected PackageAdmin getPackageAdmin()
   {
      OSGiBundleManager bundleManager = framework.getBundleManager();
      return bundleManager.getPlugin(PackageAdminPlugin.class);
   }

   protected BundleContext getSystemContext()
   {
      BundleContext sysContext = framework.getBundleContext();
      return sysContext;
   }

   protected void assertLoaderBundle(Bundle expLoader, Bundle srcLoader, Class<?> clazz) throws ClassNotFoundException
   {
      Class<?> classA = srcLoader.loadClass(clazz.getName());
      Bundle wasBundle = getPackageAdmin().getBundle(classA);
      assertEquals("Expected loader bundle", expLoader.getSymbolicName(), wasBundle.getSymbolicName());
   }

   protected void assertLoadFails(Bundle srcLoader, Class<?> clazz)
   {
      try
      {
         srcLoader.loadClass(clazz.getName());
         fail("ClassNotFoundException expected");
      }
      catch (ClassNotFoundException ex)
      {
         // expected
      }
   }
}