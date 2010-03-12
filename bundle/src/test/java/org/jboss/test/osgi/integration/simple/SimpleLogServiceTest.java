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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import org.jboss.osgi.spi.util.ServiceLoader;
import org.jboss.osgi.testing.OSGiRuntimeTest;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * A test that deployes a bundle and verifies its state
 * 
 * @author thomas.diesler@jboss.com
 * @since 18-Aug-2009
 */
public class SimpleLogServiceTest extends OSGiRuntimeTest
{
   @Before
   public void setUp()
   {
      System.clearProperty("simple-logservice-bundle");
   }

   @Test
   public void testNoLogService() throws Exception
   {
      FrameworkFactory factory = ServiceLoader.loadService(FrameworkFactory.class);
      Framework framework = factory.newFramework(null);
      framework.start();

      BundleContext sysContext = framework.getBundleContext();
      Bundle bundle = sysContext.installBundle(getTestArchivePath("simple-logservice-bundle.jar"));

      try
      {
         bundle.start();
         fail("Unresolved package contstraint on [org.osgi.service.log] expected");
      }
      catch (BundleException ex)
      {
         // expected
      }

      framework.stop();
   }

   @Test
   public void testLogServiceFromThirdParty() throws Exception
   {
      FrameworkFactory factory = ServiceLoader.loadService(FrameworkFactory.class);
      Framework framework = factory.newFramework(null);
      framework.start();

      BundleContext sysContext = framework.getBundleContext();
      sysContext.installBundle(getTestArchivePath("bundles/org.apache.felix.log.jar")).start();
      
      Bundle bundle = sysContext.installBundle(getTestArchivePath("simple-logservice-bundle.jar"));
      try
      {
         bundle.start();
      }
      catch (BundleException ex)
      {
         // Expected UNRESOLVED OSGiPackageRequirement{org.osgi.util.tracker [0.0.0,?)} with MC Framework
      }

      assumeTrue(bundle.getState() == Bundle.ACTIVE);
      
      // The bundle activator is expected to set this property
      String result = System.getProperty(bundle.getSymbolicName());
      assertNotNull("Result property not null", result);

      assertTrue("BundleActivator start", result.indexOf("startBundleActivator") > 0);
      assertFalse("getService", result.indexOf("getService") > 0);
      assertFalse("addingService", result.indexOf("addingService") > 0);
      
      framework.stop();
   }

   @Test
   public void testLogServiceFromCompendium() throws Exception
   {
      FrameworkFactory factory = ServiceLoader.loadService(FrameworkFactory.class);
      Framework framework = factory.newFramework(null);
      framework.start();

      BundleContext sysContext = framework.getBundleContext();
      sysContext.installBundle(getTestArchivePath("bundles/org.osgi.compendium.jar"));
      
      Bundle bundle = sysContext.installBundle(getTestArchivePath("simple-logservice-bundle.jar"));
      bundle.start();

      // The bundle activator is expected to set this property
      String result = System.getProperty(bundle.getSymbolicName());
      assertNotNull("Result property not null", result);

      assertTrue("BundleActivator start", result.indexOf("startBundleActivator") > 0);
      assertFalse("getService", result.indexOf("getService") > 0);
      assertFalse("addingService", result.indexOf("addingService") > 0);

      framework.stop();
   }

   @Test
   public void testLogServiceFromTwoExporters() throws Exception
   {
      FrameworkFactory factory = ServiceLoader.loadService(FrameworkFactory.class);
      Framework framework = factory.newFramework(null);
      framework.start();

      BundleContext sysContext = framework.getBundleContext();
      sysContext.installBundle(getTestArchivePath("bundles/org.osgi.compendium.jar")).start();
      sysContext.installBundle(getTestArchivePath("bundles/org.apache.felix.log.jar")).start();

      Bundle bundle = sysContext.installBundle(getTestArchivePath("simple-logservice-bundle.jar"));
      bundle.start();

      // The bundle activator is expected to set this property
      String result = System.getProperty(bundle.getSymbolicName());
      assertNotNull("Result property not null", result);

      assertTrue("BundleActivator start", result.indexOf("startBundleActivator") > 0);
      assertTrue("getService", result.indexOf("getService") > 0);
      assertTrue("addingService", result.indexOf("addingService") > 0);

      framework.stop();
   }
}