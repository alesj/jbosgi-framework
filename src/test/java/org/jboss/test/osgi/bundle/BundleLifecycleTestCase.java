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

import junit.framework.Test;

import org.jboss.test.osgi.FrameworkTest;
import org.jboss.test.osgi.bundle.support.a.FailOnStartActivator;
import org.jboss.test.osgi.bundle.support.b.LifecycleService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * BundleLifecycleTestCase.
 *
 * @author thomas.Diesler@jboss.com
 * @since 15-Dec-2009
 */
public class BundleLifecycleTestCase extends FrameworkTest
{
   public static Test suite()
   {
      return suite(BundleLifecycleTestCase.class);
   }

   public BundleLifecycleTestCase(String name)
   {
      super(name);
   }

   /**
    * Verifies that the service bundle can get started
    */
   public void testSimpleStart() throws Exception
   {
      Bundle bundleA = installBundle(assembleBundle("lifecycle-service", "/bundles/lifecycle/simple-service", LifecycleService.class));
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
   public void testDependencyNotAvailable() throws Exception
   {
      Bundle bundleA = installBundle(assembleBundle("lifecycle-service", "/bundles/lifecycle/simple-service", LifecycleService.class));
      try
      {
         assertBundleState(Bundle.INSTALLED, bundleA.getState());

         // BundleA not started - service not available  
         ServiceReference sref = getSystemBundle().getBundleContext().getServiceReference(LifecycleService.class.getName());
         assertNull("Service not available", sref);

         Bundle bundleB = installBundle(assembleBundle("lifecycle-failstart", "/bundles/lifecycle/fail-on-start", FailOnStartActivator.class));
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
   public void testDependencyAvailable() throws Exception
   {
      Bundle bundleA = installBundle(assembleBundle("lifecycle-service", "/bundles/lifecycle/simple-service", LifecycleService.class));
      try
      {
         bundleA.start();
         assertBundleState(Bundle.ACTIVE, bundleA.getState());

         Bundle bundleB = installBundle(assembleBundle("lifecycle-failstart", "/bundles/lifecycle/fail-on-start", FailOnStartActivator.class));
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
   public void testStartRetry() throws Exception
   {
      Bundle bundleA = installBundle(assembleBundle("lifecycle-service", "/bundles/lifecycle/simple-service", LifecycleService.class));
      try
      {
         assertBundleState(Bundle.INSTALLED, bundleA.getState());

         Bundle bundleB = installBundle(assembleBundle("lifecycle-failstart", "/bundles/lifecycle/fail-on-start", FailOnStartActivator.class));
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
   public void testFailToResolve() throws Exception
   {
      Bundle bundleB = installBundle(assembleBundle("lifecycle-failstart", "/bundles/lifecycle/fail-on-start", FailOnStartActivator.class));
      try
      {
         assertBundleState(Bundle.INSTALLED, bundleB.getState());

         // Get the PackageAdmin service
         BundleContext sysContext = getSystemBundle().getBundleContext();
         ServiceReference sref = sysContext.getServiceReference(PackageAdmin.class.getName());
         PackageAdmin packageAdmin = (PackageAdmin)sysContext.getService(sref);
         
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
   public void testInstallInvalid() throws Exception
   {
      try
      {
         installBundle(assembleBundle("missing-symbolic-name", "/bundles/lifecycle/invalid01"));
         fail("BundleException expected");
      }
      catch (BundleException ex)
      {
         // expected
      }
      
      try
      {
         installBundle(assembleBundle("invalid-export", "/bundles/lifecycle/invalid02"));
         fail("BundleException expected");
      }
      catch (BundleException ex)
      {
         // expected
      }
   }
}
