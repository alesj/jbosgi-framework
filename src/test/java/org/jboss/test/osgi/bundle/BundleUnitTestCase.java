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
package org.jboss.test.osgi.bundle;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import junit.framework.Test;

import org.jboss.test.osgi.FrameworkTest;
import org.jboss.virtual.VFSUtils;
import org.jboss.virtual.VirtualFile;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * BundleUnitTestCase.
 *
 * TODO test security
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class BundleUnitTestCase extends FrameworkTest
{
   public static Test suite()
   {
      return suite(BundleUnitTestCase.class);
   }

   public BundleUnitTestCase(String name)
   {
      super(name);
   }

   public void testBundleId() throws Exception
   {
      long id1 = -1;
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         id1 = bundle.getBundleId();
      }
      finally
      {
         uninstall(bundle);
      }
      assertEquals(id1, bundle.getBundleId());

      long id2 = -1;
      bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         id2 = bundle.getBundleId();
      }
      finally
      {
         uninstall(bundle);
      }
      assertEquals(id2, bundle.getBundleId());
      assertTrue("Ids should be different" + id1 + "," + id2, id1 != id2);
   }
   
   public void testSymbolicName() throws Exception
   {
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         assertEquals("org.jboss.test.osgi.simple1", bundle.getSymbolicName());
      }
      finally
      {
         uninstall(bundle);
      }
      assertEquals("org.jboss.test.osgi.simple1", bundle.getSymbolicName());
   }
   
   public void testState() throws Exception
   {
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         assertEquals(Bundle.INSTALLED, bundle.getState());

         bundle.start();
         assertEquals(Bundle.ACTIVE, bundle.getState());

         bundle.stop();
         assertEquals(Bundle.RESOLVED, bundle.getState());
      }
      finally
      {
         uninstall(bundle);
      }
      assertEquals(Bundle.UNINSTALLED, bundle.getState());
   }
   
   public void testGetBundleContext() throws Exception
   {
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         BundleContext bundleContext = bundle.getBundleContext();
         assertNull(bundleContext);
         
         bundle.start();
         bundleContext = bundle.getBundleContext();
         assertNotNull(bundleContext);
         
         bundle.stop();
         bundleContext = bundle.getBundleContext();
         assertNull(bundleContext);
      }
      finally
      {
         uninstall(bundle);
      }
   }
   
   public void testLastModified() throws Exception
   {
      // TODO testLastModified
   }
   
   public void testStartStop() throws Exception
   {
      // TODO testStartStop
   }
   
   public void testUpdate() throws Exception
   {
      VirtualFile assemble1 = assembleBundle("bundle1", "/bundles/update/update-bundle1");
      VirtualFile assemble2 = assembleBundle("bundle2", "/bundles/update/update-bundle2");
      
      Manifest manifest = VFSUtils.getManifest(assemble2);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      new JarOutputStream(baos, manifest).close();
      ByteArrayInputStream updateStream = new ByteArrayInputStream(baos.toByteArray());
      
      // [JBVFS-130] VFSUtils.temp(assembledDirectory) cannot create tmp dir
      // assemble2 = VFSUtils.temp(assemble2);
      
      Bundle bundle = installBundle(assemble1);
      try
      {
         int beforeCount = getBundleManager().getBundles().size();
         
         bundle.start();
         assertBundleState(Bundle.ACTIVE, bundle.getState());
         assertEquals("Bundle-Version", "1.0.0", bundle.getHeaders().get(Constants.BUNDLE_VERSION));
         
         bundle.update(updateStream);
         assertBundleState(Bundle.ACTIVE, bundle.getState());
         assertEquals("Bundle-Version", "1.0.1", bundle.getHeaders().get(Constants.BUNDLE_VERSION));
         
         int afterCount = getBundleManager().getBundles().size();
         assertEquals("Bundle count", beforeCount, afterCount);
      }
      finally
      {
         bundle.uninstall();
      }
   }
   
   public void testUninstall() throws Exception
   {
      // TODO testUninstall
   }
   
   public void testSingleton() throws Exception
   {
      Bundle bundle1 = installBundle(assembleBundle("bundle10", "/bundles/singleton/singleton1"));
      try
      {
         Bundle bundle2 = installBundle(assembleBundle("bundle20", "/bundles/singleton/singleton2"));
         uninstall(bundle2);
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(BundleException.class, t);
      }
      finally
      {
         uninstall(bundle1);
      }
   }
   
   public void testNotSingleton() throws Exception
   {
      Bundle bundle1 = installBundle(assembleBundle("bundle1", "/bundles/singleton/singleton1"));
      try
      {
         Bundle bundle2 = installBundle(assembleBundle("not-singleton", "/bundles/singleton/not-singleton"));
         try
         {
            assertEquals(bundle1.getSymbolicName(), bundle2.getSymbolicName());
         }
         finally
         {
            uninstall(bundle2);
         }
      }
      finally
      {
         uninstall(bundle1);
      }
   }
   
   @SuppressWarnings({ "rawtypes", "unchecked" })
   public void testGetHeaders() throws Exception
   {
      // TODO case insensistive
      Bundle bundle = addBundle("/bundles/simple/", "simple-bundle1");
      try
      {
         Dictionary expected = new Hashtable();
         expected.put(Constants.BUNDLE_NAME, "Simple1");
         expected.put(Constants.BUNDLE_SYMBOLICNAME, "org.jboss.test.osgi.simple1");
         expected.put(Constants.BUNDLE_MANIFESTVERSION, "2");
         expected.put(Attributes.Name.MANIFEST_VERSION.toString(), "1.0");
         expected.put(Attributes.Name.IMPLEMENTATION_TITLE.toString(), "JBoss OSGi tests");
         expected.put(Attributes.Name.IMPLEMENTATION_VENDOR.toString(), "jboss.org");
         expected.put(Attributes.Name.IMPLEMENTATION_VERSION.toString(), "test");
         
         Dictionary dictionary = bundle.getHeaders();
         assertEquals(expected, dictionary);
      }
      finally
      {
         uninstall(bundle);
      }
   }
   
   public void testLocation() throws Exception
   {
      // TODO testGetLocation
   }
   
   public void testGetRegisteredServices() throws Exception
   {
      // TODO testGetRegisteredServices
   }
   
   public void testServicesInUse() throws Exception
   {
      // TODO testServicesInUse
   }
   
   public void testHasPermission() throws Exception
   {
      // TODO testHasPermission
   }
   
   public void testGetResources() throws Exception
   {
      // TODO testGetResource(s)
   }
   
   public void testLoadClass() throws Exception
   {
      // TODO testLoadClass
   }
}
