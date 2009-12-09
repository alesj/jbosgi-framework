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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jboss.osgi.spi.framework.OSGiBootstrap;
import org.jboss.osgi.spi.framework.OSGiBootstrapProvider;
import org.jboss.osgi.testing.OSGiTest;
import org.jboss.test.osgi.classloader.support.a.A;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.launch.Framework;

/**
 * BundleClassPathTest.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 07-Oct-2009
 */
public class BundleClassPathTest extends OSGiTest
{
   @Test
   public void testBundleClassPath() throws Exception
   {
      OSGiBootstrapProvider bootProvider = OSGiBootstrap.getBootstrapProvider();
      Framework framework = bootProvider.getFramework();
      framework.start();

      BundleContext sysContext = framework.getBundleContext();
      Bundle bundle = sysContext.installBundle(getTestArchivePath("bundle-classpath.war"));

      bundle.start();
      assertEquals("Bundle state", Bundle.ACTIVE, bundle.getState());

      Class<?> clazz = bundle.loadClass(A.class.getName());
      assertNotNull("Loaded class", clazz);
      
      bundle.uninstall();
      assertEquals("Bundle state", Bundle.UNINSTALLED, bundle.getState());

      framework.stop();
   }
}
