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
package org.jboss.test.osgi.launch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.jboss.osgi.framework.launch.OSGiFrameworkFactory;
import org.jboss.osgi.framework.testing.AbstractFrameworkTest;
import org.jboss.shrinkwrap.api.Archive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;

/**
 * @author <a href="david@redhat.com">David Bosschaert</a>
 */
public class FrameworkStartLevelConfigurationTestCase extends AbstractFrameworkTest
{
   private Properties savedProperties;
   private File autoInstallConfigFile;
   
   @Before
   public void setUp() throws Exception
   {
      StringBuilder bs = new StringBuilder();

      ClassLoader jvmCL = Thread.currentThread().getContextClassLoader();
      bs.append(jvmCL.getResource("META-INF/jboss-osgi-bootstrap.xml").toExternalForm());
      bs.append(' ');
      bs.append(jvmCL.getResource("META-INF/jboss-osgi-bootstrap-container.xml").toExternalForm());
      bs.append(' ');
      bs.append(jvmCL.getResource("META-INF/jboss-osgi-bootstrap-system.xml").toExternalForm());
      bs.append(' ');
      bs.append(getClass().getResource("/bootstrap/startlevel/test-startlevel-1.xml").toExternalForm());
      bs.append(' ');

      Archive<?> assA = assembleArchive("lifecycle-order1", "/bundles/lifecycle/order01",
            org.jboss.test.osgi.bundle.support.lifecycle1.Activator.class);
      Archive<?> assB = assembleArchive("lifecycle-order2", "/bundles/lifecycle/order02",
            org.jboss.test.osgi.bundle.support.lifecycle2.Activator.class);
      Archive<?> assC = assembleArchive("lifecycle-order3", "/bundles/lifecycle/order03",
            org.jboss.test.osgi.bundle.support.lifecycle3.Activator.class);

      String autoInstallConfig = "<deployment xmlns='urn:jboss:bean-deployer:2.0'>" +
            "  <bean name='OSGiAutoInstallPlugin' class='org.jboss.osgi.framework.plugins.internal.AutoInstallPluginImpl'>" +
            "    <constructor><parameter><inject bean='OSGiBundleManager' /></parameter></constructor>" +
            "    <property name='autoInstall'>" +
            "      <list elementClass='java.net.URL'>" +
            "        <value>" + toVirtualFile(assA).getStreamURL().toExternalForm() + "</value>" +
            "      </list>" +
            "    </property>" +
            "    <property name='autoStart'>" +
            "      <list elementClass='java.net.URL'>" +
            "        <value>" + toVirtualFile(assB).getStreamURL().toExternalForm() + "</value>" +
            "        <value>" + toVirtualFile(assC).getStreamURL().toExternalForm() + "</value>" +
            "      </list>" +
            "    </property>" +
            "  </bean>" +
            "</deployment>";
      autoInstallConfigFile = File.createTempFile("autoInstallConfig", ".xml");
      copyStreams(new ByteArrayInputStream(autoInstallConfig.getBytes()),
            new FileOutputStream(autoInstallConfigFile));
      bs.append(autoInstallConfigFile.toURI().toString());

      savedProperties = (Properties)System.getProperties().clone();
      System.setProperty(OSGiFrameworkFactory.BOOTSTRAP_URLS, bs.toString());

      createFramework();
      getFramework().start();
   }

   @After
   public void tearDown() throws Exception
   {
      shutdownFramework();
      autoInstallConfigFile.delete();

      System.setProperties(savedProperties);
   }

   @Test
   public void testStartlevelConfiguration() throws Exception
   {
      BundleContext ctx = getFramework().getBundleContext();
      final Bundle b2 = findBundle(ctx, "lifecycle-order02");
      assertTrue(b2.getState() != Bundle.ACTIVE);
      final Bundle b3 = findBundle(ctx, "lifecycle-order03");
      assertTrue(b3.getState() != Bundle.ACTIVE);

      final CountDownLatch b2Latch = new CountDownLatch(1);
      final CountDownLatch b3Latch = new CountDownLatch(1);
      BundleListener bl = new BundleListener()
         {
            @Override
            public void bundleChanged(BundleEvent event)
            {
               if (event.getType() == BundleEvent.STARTED)
               {
                  if (event.getBundle().equals(b2))
                     b2Latch.countDown();
                  if (event.getBundle().equals(b3))
                     b3Latch.countDown();
               }
            }
         };
      ctx.addBundleListener(bl);

      ServiceReference sref = ctx.getServiceReference(StartLevel.class.getName());
      StartLevel sls = (StartLevel)ctx.getService(sref);
      sls.setStartLevel(3);

      b2Latch.await(10, SECONDS);
      assertEquals(Bundle.ACTIVE, b2.getState());
      assertTrue(b3.getState() != Bundle.ACTIVE);

      sls.setStartLevel(4);

      b3Latch.await(10, SECONDS);
      assertEquals(Bundle.ACTIVE, b2.getState());
      assertEquals(Bundle.ACTIVE, b3.getState());
   }

   private Bundle findBundle(BundleContext ctx, String bsn)
   {
      for (Bundle b : ctx.getBundles())
         if (bsn.equals(b.getSymbolicName()))
            return b;

      return null;
   }

   private static void copyStreams(InputStream in, OutputStream out) throws IOException
   {
      try
      {
         int c;
         while ((c = in.read()) != -1)
            out.write(c);
      }
      finally
      {
         in.close();
         out.close();
      }
   }
}
