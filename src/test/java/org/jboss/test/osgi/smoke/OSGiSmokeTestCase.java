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
package org.jboss.test.osgi.smoke;

import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import junit.framework.Test;

import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.internal.AbstractOSGiMetaData;
import org.jboss.test.osgi.FrameworkTest;
import org.jboss.test.osgi.smoke.support.a.A;
import org.jboss.test.osgi.smoke.support.a.b.B;
import org.jboss.test.osgi.smoke.support.c.C;
import org.osgi.framework.Bundle;

/**
 * OSGiSmokeTestCase.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class OSGiSmokeTestCase extends FrameworkTest
{
   public static Test suite()
   {
      return suite(OSGiSmokeTestCase.class);
   }

   public OSGiSmokeTestCase(String name)
   {
      super(name);
   }

   public void testNoManifest() throws Exception
   {
      // TODO [JBOSGI-203] Define non OSGi bundle handling by the Framework
      // testBundle("smoke-no-manifest", Bundle.ACTIVE);
   }

   public void testNonOSGiManifest() throws Exception
   {
      // TODO [JBOSGI-203] Define non OSGi bundle handling by the Framework
      // testBundle("smoke-non-osgi-manifest", Bundle.ACTIVE);
   }

   public void testOSGiManifest() throws Exception
   {
      testBundle("smoke-osgi-manifest", Bundle.INSTALLED);
   }

   public void testAssembled() throws Exception
   {
      Bundle bundle = assembleBundle("smoke-assembled", "/bundles/smoke/smoke-assembled", A.class);
      try
      {
         testBundle(bundle, "smoke-assembled", Bundle.INSTALLED);
         bundle.start();
         assertLoadClass(bundle, A.class);
         assertLoadClassFail(bundle, B.class);
         assertLoadClassFail(bundle, C.class);
      }
      finally
      {
         bundle.uninstall();
      }
   }

   public void testDeployedNoManifest() throws Exception
   {
      // TODO [JBOSGI-203] Define non OSGi bundle handling by the Framework
      // testDeployedBundle("smoke-no-manifest", Bundle.ACTIVE);
   }

   public void testDeployedNonOSGiManifest() throws Exception
   {
      // TODO [JBOSGI-203] Define non OSGi bundle handling by the Framework
      // testDeployedBundle("smoke-non-osgi-manifest", Bundle.ACTIVE);
   }

   public void testDeployedOSGiManifest() throws Exception
   {
      testDeployedBundle("smoke-osgi-manifest", Bundle.INSTALLED);
   }

   public void testAssembledDeployment() throws Exception
   {
      Manifest manifest = new Manifest();
      Attributes attributes = manifest.getMainAttributes();
      attributes.putValue("Bundle-Name", "SmokeDeployment");
      attributes.putValue("Bundle-SymbolicName", "org.jboss.test.osgi.smoke.deployment");
      OSGiMetaData metaData = new AbstractOSGiMetaData(manifest);
      Bundle bundle = deployBundle("smoke-deployment", metaData, A.class);
      try
      {
         assertEquals(Bundle.INSTALLED, bundle.getState());
         bundle.start();
         assertLoadClass(bundle, A.class);
         assertLoadClassFail(bundle, B.class);
         assertLoadClassFail(bundle, C.class);
      }
      finally
      {
         bundle.uninstall();
      }
   }

   public void testAssembledNonOSGiDeployment() throws Exception
   {
      /* TODO [JBOSGI-203] Define non OSGi bundle handling by the Framework
      Bundle bundle = deployBundle("smoke-non-osgi-deployment", A.class);
      try
      {
         assertEquals(Bundle.ACTIVE, bundle.getState());
         assertLoadClass(bundle, A.class);
         assertLoadClassFail(bundle, B.class);
         assertLoadClassFail(bundle, C.class);
      }
      finally
      {
         bundle.uninstall();
      }
      */
   }

   protected void testBundle(String name, int expectedState) throws Exception
   {
      Bundle bundle = addBundle("/bundles/smoke/", name);
      try
      {
         testBundle(bundle, name, expectedState);
      }
      finally
      {
         bundle.uninstall();
      }
   }

   protected void testDeployedBundle(String name, int expectedState) throws Exception
   {
      Bundle bundle = deployBundle("/bundles/smoke/", name);
      try
      {
         testBundle(bundle, name, expectedState);
      }
      finally
      {
         bundle.uninstall();
      }
   }

   protected void testBundle(Bundle bundle, String name, int expectedState) throws Exception
   {
      assertEquals(expectedState, bundle.getState());
      checkId(bundle, name);
      bundle.start();
      bundle.stop();
   }

   protected void checkId(Bundle bundle, String name) throws Exception
   {
      URL url = bundle.getEntry("id");
      if (url == null)
         fail("id entry not found for " + bundle);
      InputStream is = url.openStream();
      byte[] bytes = new byte[100];
      is.read(bytes);
      String value = new String(bytes);
      assertTrue("Expected=" + name + " was " + value, value.startsWith(name));
   }
}
