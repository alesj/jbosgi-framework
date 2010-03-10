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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.test.osgi.AbstractFrameworkTest;
import org.jboss.test.osgi.bundle.support.a.FailOnStartActivator;
import org.jboss.test.osgi.bundle.support.b.LifecycleService;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * BundleLifecycleTestCase.
 *
 * @author thomas.Diesler@jboss.com
 * @since 15-Dec-2009
 */
public class BundleLifecycleTestCase extends AbstractFrameworkTest
{

   /**
    * Verifies that the service bundle can get started
    */
   @Test
   public void testSimpleStart() throws Exception
   {
      VirtualFile assemblyA = assembleArchive("lifecycle-service", "/bundles/lifecycle/simple-service", LifecycleService.class);
      Bundle bundleA = context.installBundle(assemblyA.toURL().toExternalForm());
      try
      {
         assertBundleState(Bundle.INSTALLED, bundleA.getState());

         bundleA.start();
         assertBundleState(Bundle.ACTIVE, bundleA.getState());

         ServiceReference sref = bundleA.getBundleContext().getServiceReference(LifecycleService.class.getName());
         assertNotNull("Service available", sref);
      }
      finally
      {
         bundleA.uninstall();
         assertBundleState(Bundle.UNINSTALLED, bundleA.getState());
      }
   }

   /**
    * Verifies that the bundle state is RESOLVED after a failure in BundleActivator.start()
    */
   @Test
   public void testDependencyNotAvailable() throws Exception
   {
      VirtualFile assemblyA = assembleArchive("lifecycle-service", "/bundles/lifecycle/simple-service", LifecycleService.class);
      Bundle bundleA = context.installBundle(assemblyA.toURL().toExternalForm());
      try
      {
         assertBundleState(Bundle.INSTALLED, bundleA.getState());

         // BundleA not started - service not available  
         ServiceReference sref = context.getServiceReference(LifecycleService.class.getName());
         assertNull("Service not available", sref);

         VirtualFile assemblyB = assembleArchive("lifecycle-failstart", "/bundles/lifecycle/fail-on-start", FailOnStartActivator.class);
         Bundle bundleB = context.installBundle(assemblyB.toURL().toExternalForm());
         try
         {
            assertBundleState(Bundle.INSTALLED, bundleB.getState());

            bundleB.start();
            fail("BundleException expected");
         }
         catch (BundleException ex)
         {
            assertBundleState(Bundle.RESOLVED, bundleB.getState());
         }
         finally
         {
            bundleB.uninstall();
            assertBundleState(Bundle.UNINSTALLED, bundleB.getState());
         }
      }
      finally
      {
         bundleA.uninstall();
         assertBundleState(Bundle.UNINSTALLED, bundleA.getState());
      }
   }

   /**
    * Verifies that BundleB can get started when the service is available
    */
   @Test
   public void testDependencyAvailable() throws Exception
   {
      VirtualFile assemblyA = assembleArchive("lifecycle-service", "/bundles/lifecycle/simple-service", LifecycleService.class);
      Bundle bundleA = context.installBundle(assemblyA.toURL().toExternalForm());
      try
      {
         bundleA.start();
         assertBundleState(Bundle.ACTIVE, bundleA.getState());

         VirtualFile assemblyB = assembleArchive("lifecycle-failstart", "/bundles/lifecycle/fail-on-start", FailOnStartActivator.class);
         Bundle bundleB = context.installBundle(assemblyB.toURL().toExternalForm());
         try
         {
            bundleB.start();
            assertBundleState(Bundle.ACTIVE, bundleB.getState());
         }
         finally
         {
            bundleB.uninstall();
            assertBundleState(Bundle.UNINSTALLED, bundleB.getState());
         }
      }
      finally
      {
         bundleA.uninstall();
         assertBundleState(Bundle.UNINSTALLED, bundleA.getState());
      }
   }

   /**
    * Verifies that BundleB can get started when the service is made available 
    */
   @Test
   public void testStartRetry() throws Exception
   {
      VirtualFile assemblyA = assembleArchive("lifecycle-service", "/bundles/lifecycle/simple-service", LifecycleService.class);
      Bundle bundleA = context.installBundle(assemblyA.toURL().toExternalForm());
      try
      {
         assertBundleState(Bundle.INSTALLED, bundleA.getState());

         VirtualFile assemblyB = assembleArchive("lifecycle-failstart", "/bundles/lifecycle/fail-on-start", FailOnStartActivator.class);
         Bundle bundleB = context.installBundle(assemblyB.toURL().toExternalForm());
         try
         {
            assertBundleState(Bundle.INSTALLED, bundleB.getState());

            try
            {
               bundleB.start();
               fail("BundleException expected");
            }
            catch (BundleException ex)
            {
               assertBundleState(Bundle.RESOLVED, bundleB.getState());

               // Now, make the service available
               bundleA.start();
               assertBundleState(Bundle.ACTIVE, bundleA.getState());
            }

            // BundleB can now be started
            bundleB.start();
            assertBundleState(Bundle.ACTIVE, bundleB.getState());
         }
         finally
         {
            bundleB.uninstall();
            assertBundleState(Bundle.UNINSTALLED, bundleB.getState());
         }
      }
      finally
      {
         bundleA.uninstall();
         assertBundleState(Bundle.UNINSTALLED, bundleA.getState());
      }
   }

   /**
    * Verifies that BundleB is still INSTALLED after a failure in PackageAdmin.resolve()
    */
   @Test
   public void testFailToResolve() throws Exception
   {
      VirtualFile assemblyA = assembleArchive("lifecycle-failstart", "/bundles/lifecycle/fail-on-start", FailOnStartActivator.class);
      Bundle bundleB = context.installBundle(assemblyA.toURL().toExternalForm());
      try
      {
         assertBundleState(Bundle.INSTALLED, bundleB.getState());

         // Get the PackageAdmin service
         ServiceReference sref = context.getServiceReference(PackageAdmin.class.getName());
         PackageAdmin packageAdmin = (PackageAdmin)context.getService(sref);
         
         // Attempt to explicitly resolve a bundle with missing dependency 
         boolean allResolved = packageAdmin.resolveBundles(new Bundle[] { bundleB });
         assertFalse("Resolve fails", allResolved);
         
         // Verify that the bundkle is still in state INSTALLED
         assertBundleState(Bundle.INSTALLED, bundleB.getState());
      }
      finally
      {
         bundleB.uninstall();
         assertBundleState(Bundle.UNINSTALLED, bundleB.getState());
      }
   }

   /**
    * Verifies that we get a BundleException when an invalid bundle is installed
    */
   @Test
   public void testInstallInvalid() throws Exception
   {
      try
      {
         VirtualFile assembly = assembleArchive("missing-symbolic-name", "/bundles/lifecycle/invalid01");
         context.installBundle(assembly.toURL().toExternalForm());
         fail("BundleException expected");
      }
      catch (BundleException ex)
      {
         // expected
      }
      
      try
      {
         VirtualFile assembly = assembleArchive("invalid-export", "/bundles/lifecycle/invalid02");
         context.installBundle(assembly.toURL().toExternalForm());
         fail("BundleException expected");
      }
      catch (BundleException ex)
      {
         // expected
      }
   }
}
