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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.jboss.osgi.vfs.VFSUtils;
import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.test.osgi.AbstractFrameworkTest;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * BundleUnitTestCase.
 *
 * TODO test security
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class BundleUnitTestCase extends AbstractFrameworkTest
{
   @Test
   public void testBundleId() throws Exception
   {
      long id1 = -1;
      VirtualFile assembly = assembleArchive("simple-bundle1", "/bundles/simple/simple-bundle1", new Class[0]);
      Bundle bundle = context.installBundle(assembly.toURL().toExternalForm());
      try
      {
         id1 = bundle.getBundleId();
      }
      finally
      {
         bundle.uninstall();
      }
      assertEquals(id1, bundle.getBundleId());

      long id2 = -1;
      bundle = context.installBundle(assembly.toURL().toExternalForm());
      try
      {
         id2 = bundle.getBundleId();
      }
      finally
      {
         bundle.uninstall();
      }
      assertEquals(id2, bundle.getBundleId());
      assertTrue("Ids should be different" + id1 + "," + id2, id1 != id2);
   }
   
   @Test
   public void testSymbolicName() throws Exception
   {
      VirtualFile assembly = assembleArchive("simple-bundle1", "/bundles/simple/simple-bundle1", new Class[0]);
      Bundle bundle = context.installBundle(assembly.toURL().toExternalForm());
      try
      {
         assertEquals("org.jboss.test.osgi.simple1", bundle.getSymbolicName());
      }
      finally
      {
         bundle.uninstall();
      }
      assertEquals("org.jboss.test.osgi.simple1", bundle.getSymbolicName());
   }
   
   @Test
   public void testState() throws Exception
   {
      VirtualFile assembly = assembleArchive("simple-bundle1", "/bundles/simple/simple-bundle1", new Class[0]);
      Bundle bundle = context.installBundle(assembly.toURL().toExternalForm());
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
         bundle.uninstall();
      }
      assertEquals(Bundle.UNINSTALLED, bundle.getState());
   }
   
   @Test
   public void testGetBundleContext() throws Exception
   {
      VirtualFile assembly = assembleArchive("simple-bundle1", "/bundles/simple/simple-bundle1", new Class[0]);
      Bundle bundle = context.installBundle(assembly.toURL().toExternalForm());
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
         bundle.uninstall();
      }
   }
   
   @Test
   public void testLastModified() throws Exception
   {
      // TODO testLastModified
   }
   
   @Test
   public void testStartStop() throws Exception
   {
      // TODO testStartStop
   }
   
   @Test
   public void testUpdate() throws Exception
   {
      VirtualFile assemble1 = assembleArchive("bundle1", "/bundles/update/update-bundle1");
      VirtualFile assemble2 = assembleArchive("bundle2", "/bundles/update/update-bundle2");
      
      Manifest manifest = VFSUtils.getManifest(assemble2);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      new JarOutputStream(baos, manifest).close();
      ByteArrayInputStream updateStream = new ByteArrayInputStream(baos.toByteArray());
      
      Bundle bundle = context.installBundle(assemble1.toURL().toExternalForm());
      try
      {
         int beforeCount = context.getBundles().length;
         
         bundle.start();
         assertBundleState(Bundle.ACTIVE, bundle.getState());
         assertEquals("Bundle-Version", "1.0.0", bundle.getHeaders().get(Constants.BUNDLE_VERSION));
         
         bundle.update(updateStream);
         assertBundleState(Bundle.ACTIVE, bundle.getState());
         assertEquals("Bundle-Version", "1.0.1", bundle.getHeaders().get(Constants.BUNDLE_VERSION));
         
         int afterCount = context.getBundles().length;
         assertEquals("Bundle count", beforeCount, afterCount);
      }
      finally
      {
         bundle.uninstall();
      }
   }
   
   @Test
   public void testUninstall() throws Exception
   {
      // TODO testUninstall
   }
   
   @Test
   public void testSingleton() throws Exception
   {
      VirtualFile assemblyA = assembleArchive("bundle10", "/bundles/singleton/singleton1");
      Bundle bundleA = context.installBundle(assemblyA.toURL().toExternalForm());
      try
      {
         VirtualFile assemblyB = assembleArchive("bundle20", "/bundles/singleton/singleton2");
         Bundle bundleB = context.installBundle(assemblyB.toURL().toExternalForm());
         bundleB.uninstall();
         fail("Should not be here!");
      }
      catch (BundleException t)
      {
         // expected
      }
      finally
      {
         bundleA.uninstall();
      }
   }
   
   @Test
   public void testNotSingleton() throws Exception
   {
      VirtualFile assemblyA = assembleArchive("bundle1", "/bundles/singleton/singleton1");
      Bundle bundleA = context.installBundle(assemblyA.toURL().toExternalForm());
      try
      {
         VirtualFile assemblyB = assembleArchive("not-singleton", "/bundles/singleton/not-singleton");
         Bundle bundleB = context.installBundle(assemblyB.toURL().toExternalForm());
         try
         {
            assertEquals(bundleA.getSymbolicName(), bundleB.getSymbolicName());
         }
         finally
         {
            bundleB.uninstall();
         }
      }
      finally
      {
         bundleA.uninstall();
      }
   }
   
   @Test
   @SuppressWarnings({ "rawtypes", "unchecked" })
   public void testGetHeaders() throws Exception
   {
      VirtualFile assembly = assembleArchive("simple-bundle1", "/bundles/simple/simple-bundle1", new Class[0]);
      Bundle bundle = context.installBundle(assembly.toURL().toExternalForm());
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
         bundle.uninstall();
      }
   }
   
   @Test
   public void testLocation() throws Exception
   {
      // TODO testGetLocation
   }
   
   @Test
   public void testGetRegisteredServices() throws Exception
   {
      // TODO testGetRegisteredServices
   }
   
   @Test
   public void testServicesInUse() throws Exception
   {
      // TODO testServicesInUse
   }
   
   @Test
   public void testHasPermission() throws Exception
   {
      // TODO testHasPermission
   }
   
   @Test
   public void testGetResources() throws Exception
   {
      // TODO testGetResource(s)
   }
   
   @Test
   public void testLoadClass() throws Exception
   {
      // TODO testLoadClass
   }
}
