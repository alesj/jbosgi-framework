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
package org.jboss.test.osgi.integration.simple;

//$Id$

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jboss.osgi.spi.util.ServiceLoader;
import org.jboss.osgi.testing.OSGiBundle;
import org.jboss.osgi.testing.OSGiRuntime;
import org.jboss.osgi.testing.OSGiRuntimeHelper;
import org.jboss.test.osgi.integration.simple.bundleA.SimpleService;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * A test that deployes a bundle and verifies its state
 * 
 * @author thomas.diesler@jboss.com
 * @since 18-Aug-2009
 */
public class SimpleBundleTestCase
{
   @Test
   public void testBundleInstallLauchAPI() throws Exception
   {
      // Uses the OSGi Framework launch API
      FrameworkFactory factory = ServiceLoader.loadService(FrameworkFactory.class);
      Framework framework = factory.newFramework(null);
      framework.start();

      OSGiRuntimeHelper helper = new OSGiRuntimeHelper();

      BundleContext sysContext = framework.getBundleContext();
      Bundle bundle = sysContext.installBundle(helper.getTestArchivePath("simple-bundle.jar"));

      assertEquals("simple-bundle", bundle.getSymbolicName());

      bundle.start();
      assertEquals("Bundle state", Bundle.ACTIVE, bundle.getState());

      BundleContext bndContext = bundle.getBundleContext();
      assertNotNull("BundleContext not null", bndContext);

      // getServiceReference from bundle context
      ServiceReference sref = bndContext.getServiceReference(SimpleService.class.getName());
      assertNotNull("ServiceReference not null", sref);

      // getServiceReference from system context
      sref = sysContext.getServiceReference(SimpleService.class.getName());
      assertNotNull("ServiceReference not null", sref);

      bundle.uninstall();
      assertEquals("Bundle state", Bundle.UNINSTALLED, bundle.getState());

      framework.stop();
   }

   @Test
   public void testBundleInstallRuntimeAPI() throws Exception
   {
      // Uses the JBossOSGi SPI provided runtime abstraction
      OSGiRuntime runtime = new OSGiRuntimeHelper().getEmbeddedRuntime();
      OSGiBundle bundle = runtime.installBundle("simple-bundle.jar");

      assertEquals("simple-bundle", bundle.getSymbolicName());

      bundle.start();
      assertEquals("Bundle state", Bundle.ACTIVE, bundle.getState());

      bundle.uninstall();
      assertEquals("Bundle state", Bundle.UNINSTALLED, bundle.getState());

      runtime.shutdown();
   }
}