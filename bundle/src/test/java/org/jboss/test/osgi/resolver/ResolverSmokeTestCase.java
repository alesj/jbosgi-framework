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

// $Id: FrameworkLaunchTestCase.java 92733 2009-08-24 09:40:32Z thomas.diesler@jboss.com $

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.launch.OSGiFramework;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.resolver.Resolver;
import org.jboss.osgi.framework.resolver.ResolverBundle;
import org.jboss.osgi.spi.framework.OSGiBootstrap;
import org.jboss.osgi.spi.framework.OSGiBootstrapProvider;
import org.jboss.osgi.testing.OSGiRuntimeTest;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * Smoketest for the {@link Resolver}
 * 
 * @author thomas.diesler@jboss.com
 * @since 27-Jul-2009
 */
public class ResolverSmokeTestCase extends OSGiRuntimeTest
{
   @Test
   public void testRandomBundleResolution() throws BundleException
   {
      OSGiBootstrapProvider bootProvider = OSGiBootstrap.getBootstrapProvider();
      OSGiFramework framework = (OSGiFramework)bootProvider.getFramework();
      framework.start();

      try
      {
         List<String> bundlePaths = new ArrayList<String>();
         bundlePaths.add("bundles/jboss-osgi-apache-xerces.jar");
         bundlePaths.add("bundles/jboss-osgi-common.jar");
         bundlePaths.add("bundles/jboss-osgi-common-core.jar");
         bundlePaths.add("bundles/jboss-osgi-husky.jar");
         bundlePaths.add("bundles/jboss-osgi-jaxb.jar");
         bundlePaths.add("bundles/jboss-osgi-jmx.jar");
         bundlePaths.add("bundles/jboss-osgi-jndi.jar");
         bundlePaths.add("bundles/jboss-osgi-reflect.jar");
         bundlePaths.add("bundles/jboss-osgi-xml-binding.jar");
         bundlePaths.add("bundles/org.apache.felix.configadmin.jar");
         bundlePaths.add("bundles/org.apache.felix.log.jar");
         bundlePaths.add("bundles/org.apache.felix.metatype.jar");
         bundlePaths.add("bundles/org.osgi.compendium.jar");
         bundlePaths.add("bundles/pax-web-jetty-bundle.jar");

         Collections.shuffle(bundlePaths);

         List<Bundle> unresolved = new ArrayList<Bundle>();
         BundleContext sysContext = framework.getBundleContext();
         for (String path : bundlePaths)
         {
            Bundle bundle = sysContext.installBundle(getTestArchivePath(path));
            unresolved.add(bundle);
         }

         OSGiBundleManager bundleManager = framework.getBundleManager();
         Resolver resolver = bundleManager.getOptionalPlugin(ResolverPlugin.class);
         if (resolver != null)
         {
            List<ResolverBundle> installedBundles = resolver.getBundles();
            assertEquals("All bundles installed", bundlePaths.size(), installedBundles.size());

            List<ResolverBundle> resolved = resolver.resolve(unresolved);
            assertEquals("All bundles resolved", unresolved.size(), resolved.size());
         }

         System.out.println("FIXME [JBKERNEL-54] Cannot resolve circular dependencies");
         //PackageAdminPlugin packageAdmin = bundleManager.getPlugin(PackageAdminPlugin.class);
         //boolean allResolved = packageAdmin.resolveBundles(null);
         //assertTrue("All bundles resolved", allResolved);
      }
      finally
      {
         framework.stop();
      }
   }
}