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
import org.jboss.osgi.framework.plugins.PackageAdminPlugin;
import org.jboss.osgi.framework.plugins.Plugin;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.resolver.Resolver;
import org.jboss.osgi.framework.testing.AbstractFrameworkTest;
import org.junit.Before;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * An abstract {@link Resolver} test.
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public abstract class AbstractResolverTest extends AbstractFrameworkTest
{
   @Before
   public void setUp() throws Exception
   {
      Resolver installedResolver = getInstalledResolver();
      Resolver testResolver = getTestResolver();
      if (installedResolver != testResolver)
      {
         OSGiBundleManager bundleManager = getBundleManager();
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
      OSGiBundleManager bundleManager = getBundleManager();
      return bundleManager.getOptionalPlugin(ResolverPlugin.class);
   }

   protected PackageAdmin getPackageAdmin()
   {
      OSGiBundleManager bundleManager = getBundleManager();
      return bundleManager.getPlugin(PackageAdminPlugin.class);
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