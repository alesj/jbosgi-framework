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

// $Id$

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.test.osgi.classloader.support.a.A;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * An abstract resolver test.
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public abstract class AbstractImportExportTest extends AbstractResolverTest
{
   @Test
   public void testSimpleImport() throws Exception
   {
      // Bundle-SymbolicName: simpleimport
      // Import-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/simpleimport");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Bundle-SymbolicName: simpleexport
         // Export-Package: org.jboss.test.osgi.classloader.support.a
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleexport", A.class);
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertTrue("All resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            // Verify that the class load
            assertLoaderBundle(bundleB, bundleA, A.class);
            assertLoaderBundle(bundleB, bundleB, A.class);
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
   public void testSimpleImportPackageFails() throws Exception
   {
      // Bundle-SymbolicName: simpleimport
      // Import-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/simpleimport");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Resolve the installed bundles
         PackageAdmin packageAdmin = getPackageAdmin();
         boolean allResolved = packageAdmin.resolveBundles(null);
         assertFalse("Not all resolved", allResolved);

         // Verify bundle states
         assertEquals("BundleA INSTALLED", Bundle.INSTALLED, bundleA.getState());
         
         // Verify that the class load
         assertLoadFails(bundleA, A.class);
      }
      finally
      {
         bundleA.uninstall();
      }
   }

   @Test
   public void testExplicitBundleResolve() throws Exception
   {
      // Bundle-SymbolicName: simpleimport
      // Import-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/simpleimport");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Bundle-SymbolicName: simpleexport
         // Export-Package: org.jboss.test.osgi.classloader.support.a
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleexport", A.class);
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Only resolve BundleB
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(new Bundle[] { bundleB });
            assertTrue("All resolved", allResolved);
            
            // Verify bundle states
            assertEquals("BundleA INSTALLED", Bundle.INSTALLED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());
            
            // Verify that the class can be loaded
            assertLoaderBundle(bundleB, bundleA, A.class);
            assertLoaderBundle(bundleB, bundleB, A.class);
            
            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
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
   public void testSelfImportPackage() throws Exception
   {
      // Bundle-SymbolicName: selfimport
      // Export-Package: org.jboss.test.osgi.classloader.support.a
      // Import-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/selfimport", A.class);
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Resolve the installed bundles
         PackageAdmin packageAdmin = getPackageAdmin();
         boolean allResolved = packageAdmin.resolveBundles(null);
         assertTrue("All resolved", allResolved);

         // Verify bundle states
         assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());

         // Verify that the class load
         assertLoaderBundle(bundleA, bundleA, A.class);
      }
      finally
      {
         bundleA.uninstall();
      }
   }

   @Test
   public void testVersionImportPackage() throws Exception
   {
      //Bundle-SymbolicName: packageimportversion
      //Import-Package: org.jboss.test.osgi.classloader.support.a;version="[0.0.0,1.0.0]"
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/packageimportversion");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         //Bundle-SymbolicName: packageexportversion100
         //Export-Package: org.jboss.test.osgi.classloader.support.a;version=1.0.0
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/packageexportversion100", A.class);
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertTrue("All resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            // Verify that the class load
            assertLoaderBundle(bundleB, bundleA, A.class);
            assertLoaderBundle(bundleB, bundleB, A.class);
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
   public void testVersionImportPackageFails() throws Exception
   {
      //Bundle-SymbolicName: packageimportversionfails
      //Import-Package: org.jboss.test.osgi.classloader.support.a;version="[3.0,4.0)"
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/packageimportversionfails");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         //Bundle-SymbolicName: packageexportversion100
         //Export-Package: org.jboss.test.osgi.classloader.support.a;version=1.0.0
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/packageexportversion100", A.class);
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertFalse("Not all resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA INSTALLED", Bundle.INSTALLED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());
            
            // Verify that the class load
            assertLoadFails(bundleA, A.class);
            assertLoaderBundle(bundleB, bundleB, A.class);
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
   public void testOptionalImportPackage() throws Exception
   {
      //Bundle-SymbolicName: packageimportoptional
      //Import-Package: org.jboss.test.osgi.classloader.support.a;resolution:=optional
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/packageimportoptional");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Resolve the installed bundles
         PackageAdmin packageAdmin = getPackageAdmin();
         boolean allResolved = packageAdmin.resolveBundles(null);
         assertTrue("All resolved", allResolved);

         // Verify bundle states
         assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
         
         // Verify that the class load
         assertLoadFails(bundleA, A.class);
      }
      finally
      {
         bundleA.uninstall();
      }
   }

   @Test
   public void testOptionalImportPackageWired() throws Exception
   {
      //Bundle-SymbolicName: packageimportoptional
      //Import-Package: org.jboss.test.osgi.classloader.support.a;resolution:=optional
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/packageimportoptional");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Bundle-SymbolicName: simpleexport
         // Export-Package: org.jboss.test.osgi.classloader.support.a
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleexport", A.class);
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertTrue("All resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            // Verify that the class load
            assertLoaderBundle(bundleB, bundleA, A.class);
            assertLoaderBundle(bundleB, bundleB, A.class);
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
   public void testOptionalImportPackageNotWired() throws Exception
   {
      //Bundle-SymbolicName: packageimportoptional
      //Import-Package: org.jboss.test.osgi.classloader.support.a;resolution:=optional
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/packageimportoptional");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Resolve the installed bundles
         PackageAdmin packageAdmin = getPackageAdmin();
         boolean allResolved = packageAdmin.resolveBundles(null);
         assertTrue("All resolved", allResolved);

         // Bundle-SymbolicName: simpleexport
         // Export-Package: org.jboss.test.osgi.classloader.support.a
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleexport", A.class);
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            allResolved = packageAdmin.resolveBundles(null);
            assertTrue("All resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            // Verify that the class cannot be loaded from bundleA
            // because the wire could not be established when bundleA was resolved
            assertLoadFails(bundleA, A.class);
            assertLoaderBundle(bundleB, bundleB, A.class);
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
   public void testBundleNameImportPackage() throws Exception
   {
      //Bundle-SymbolicName: bundlenameimport
      //Import-Package: org.jboss.test.osgi.classloader.support.a;bundle-symbolic-name=simpleexport
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/bundlenameimport");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         //Bundle-SymbolicName: simpleexport
         //Export-Package: org.jboss.test.osgi.classloader.support.a
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleexport", A.class);
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertTrue("All resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            // Verify that the class load
            assertLoaderBundle(bundleB, bundleA, A.class);
            assertLoaderBundle(bundleB, bundleB, A.class);
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
   public void testBundleNameImportPackageFails() throws Exception
   {
      //Bundle-SymbolicName: bundlenameimport
      //Import-Package: org.jboss.test.osgi.classloader.support.a;bundle-symbolic-name=simpleexport
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/bundlenameimport");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         //Bundle-SymbolicName: sigleton;singleton:=true
         //Export-Package: org.jboss.test.osgi.classloader.support.a
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/singleton", A.class);
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertFalse("Not all resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA INSTALLED", Bundle.INSTALLED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            // Verify that the class load
            assertLoadFails(bundleA, A.class);
            assertLoaderBundle(bundleB, bundleB, A.class);
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
   public void testBundleVersionImportPackage() throws Exception
   {
      //Bundle-SymbolicName: bundleversionimport
      //Import-Package: org.jboss.test.osgi.classloader.support.a;bundle-version="[0.0.0,1.0.0)"
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/bundleversionimport");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Bundle-SymbolicName: simpleexport
         // Export-Package: org.jboss.test.osgi.classloader.support.a
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleexport", A.class);
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertTrue("All resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            // Verify that the class load
            assertLoaderBundle(bundleB, bundleA, A.class);
            assertLoaderBundle(bundleB, bundleB, A.class);
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
   public void testBundleVersionImportPackageFails() throws Exception
   {
      //Bundle-SymbolicName: bundleversionimportfails
      //Import-Package: org.jboss.test.osgi.classloader.support.a;bundle-version="[1.0.0,2.0.0)"
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/bundleversionimportfails");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Bundle-SymbolicName: simpleexport
         // Export-Package: org.jboss.test.osgi.classloader.support.a
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleexport", A.class);
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertFalse("Not all resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA INSTALLED", Bundle.INSTALLED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            // Verify that the class load
            assertLoadFails(bundleA, A.class);
            assertLoaderBundle(bundleB, bundleB, A.class);
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
   // [TODO] require bundle visibility
   public void testRequireBundle() throws Exception
   {
      //Bundle-SymbolicName: requirebundle
      //Require-Bundle: simpleexport
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/requirebundle");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Bundle-SymbolicName: simpleexport
         // Export-Package: org.jboss.test.osgi.classloader.support.a
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleexport", A.class);
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertTrue("All resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            // Verify that the class load
            assertLoaderBundle(bundleB, bundleA, A.class);
            assertLoaderBundle(bundleB, bundleB, A.class);
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
   public void testRequireBundleFails() throws Exception
   {
      //Bundle-SymbolicName: requirebundle
      //Require-Bundle: simpleexport
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/requirebundle");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Resolve the installed bundles
         PackageAdmin packageAdmin = getPackageAdmin();
         boolean allResolved = packageAdmin.resolveBundles(null);
         assertFalse("Not all resolved", allResolved);

         // Verify bundle states
         assertEquals("BundleA INSTALLED", Bundle.INSTALLED, bundleA.getState());

         // Verify that the class load
         assertLoadFails(bundleA, A.class);
      }
      finally
      {
         bundleA.uninstall();
      }
   }

   @Test
   public void testRequireBundleOptional() throws Exception
   {
      //Bundle-SymbolicName: requirebundleoptional
      //Require-Bundle: simpleexport;resolution:=optional
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/requirebundleoptional");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Resolve the installed bundles
         PackageAdmin packageAdmin = getPackageAdmin();
         boolean allResolved = packageAdmin.resolveBundles(null);
         assertTrue("All resolved", allResolved);

         // Verify bundle states
         assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
      }
      finally
      {
         bundleA.uninstall();
      }
   }

   @Test
   public void testRequireBundleVersion() throws Exception
   {
      //Bundle-SymbolicName: requirebundleversion
      //Require-Bundle: simpleexport;bundle-version="[0.0.0,1.0.0]"
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/requirebundleversion");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Bundle-SymbolicName: simpleexport
         // Export-Package: org.jboss.test.osgi.classloader.support.a
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleexport", A.class);
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertTrue("All resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            // Verify that the class load
            assertLoaderBundle(bundleB, bundleA, A.class);
            assertLoaderBundle(bundleB, bundleB, A.class);
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
   public void testRequireBundleVersionFails() throws Exception
   {
      //Bundle-SymbolicName: versionrequirebundlefails
      //Require-Bundle: simpleexport;bundle-version="[1.0.0,2.0.0)"
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/requirebundleversionfails");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Bundle-SymbolicName: simpleexport
         // Export-Package: org.jboss.test.osgi.classloader.support.a
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleexport", A.class);
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertFalse("Not all resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA INSTALLED", Bundle.INSTALLED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            // Verify that the class load
            assertLoadFails(bundleA, A.class);
            assertLoaderBundle(bundleB, bundleB, A.class);
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
   public void testPreferredExporterResolved() throws Exception
   {
      // Bundle-SymbolicName: simpleexport
      // Export-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/simpleexport", A.class);

      // Bundle-SymbolicName: simpleexportother
      // Export-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleexportother", A.class);

      // Bundle-SymbolicName: simpleimport
      // Import-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileC = assembleBundle("bundleC", "/bundles/resolver/simpleimport");

      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Resolve the installed bundles
         PackageAdmin packageAdmin = getPackageAdmin();
         boolean allResolved = packageAdmin.resolveBundles(null);
         assertTrue("All resolved", allResolved);

         // Verify bundle states
         assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());

         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            Bundle bundleC = framework.installBundle(fileC);
            try
            {
               allResolved = packageAdmin.resolveBundles(null);
               assertTrue("All resolved", allResolved);

               // Verify bundle states
               assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());
               assertEquals("BundleC RESOLVED", Bundle.RESOLVED, bundleC.getState());

               // Verify that the class load
               assertLoaderBundle(bundleA, bundleA, A.class);
               assertLoaderBundle(bundleB, bundleB, A.class);
               assertLoaderBundle(bundleA, bundleC, A.class);
            }
            finally
            {
               bundleC.uninstall();
            }
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
   public void testPreferredExporterResolvedReverse() throws Exception
   {
      // Bundle-SymbolicName: simpleexport
      // Export-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/simpleexport", A.class);

      // Bundle-SymbolicName: simpleexportother
      // Export-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleexportother", A.class);

      // Bundle-SymbolicName: simpleimport
      // Import-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileC = assembleBundle("bundleC", "/bundles/resolver/simpleimport");

      Bundle bundleB = framework.installBundle(fileB);
      try
      {
         // Resolve the installed bundles
         PackageAdmin packageAdmin = getPackageAdmin();
         boolean allResolved = packageAdmin.resolveBundles(null);
         assertTrue("All resolved", allResolved);

         // Verify bundle states
         assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

         Bundle bundleA = framework.installBundle(fileA);
         try
         {
            Bundle bundleC = framework.installBundle(fileC);
            try
            {
               allResolved = packageAdmin.resolveBundles(null);
               assertTrue("All resolved", allResolved);

               // Verify bundle states
               assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
               assertEquals("BundleC RESOLVED", Bundle.RESOLVED, bundleC.getState());

               // Verify that the class load
               assertLoaderBundle(bundleA, bundleA, A.class);
               assertLoaderBundle(bundleB, bundleB, A.class);
               assertLoaderBundle(bundleB, bundleC, A.class);
            }
            finally
            {
               bundleC.uninstall();
            }
         }
         finally
         {
            bundleA.uninstall();
         }
      }
      finally
      {
         bundleB.uninstall();
      }
   }

   @Test
   public void testPreferredExporterHigherVersion() throws Exception
   {
      //Bundle-SymbolicName: packageexportversion100
      //Export-Package: org.jboss.test.osgi.classloader.support.a;version=1.0.0
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/packageexportversion100", A.class);

      //Bundle-SymbolicName: packageexportversion200
      //Export-Package: org.jboss.test.osgi.classloader.support.a;version=2.0.0
      VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/packageexportversion200", A.class);

      // Bundle-SymbolicName: simpleimport
      // Import-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileC = assembleBundle("bundleC", "/bundles/resolver/simpleimport");

      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            Bundle bundleC = framework.installBundle(fileC);
            try
            {
               // Resolve the installed bundles
               PackageAdmin packageAdmin = getPackageAdmin();
               boolean allResolved = packageAdmin.resolveBundles(null);
               assertTrue("All resolved", allResolved);

               // Verify bundle states
               assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
               assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());
               assertEquals("BundleC RESOLVED", Bundle.RESOLVED, bundleC.getState());

               // Verify that the class load
               assertLoaderBundle(bundleA, bundleA, A.class);
               assertLoaderBundle(bundleB, bundleB, A.class);
               assertLoaderBundle(bundleB, bundleC, A.class);
            }
            finally
            {
               bundleC.uninstall();
            }
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
   public void testPreferredExporterHigherVersionReverse() throws Exception
   {
      //Bundle-SymbolicName: packageexportversion200
      //Export-Package: org.jboss.test.osgi.classloader.support.a;version=2.0.0
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/packageexportversion200", A.class);

      //Bundle-SymbolicName: packageexportversion100
      //Export-Package: org.jboss.test.osgi.classloader.support.a;version=1.0.0
      VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/packageexportversion100", A.class);

      // Bundle-SymbolicName: simpleimport
      // Import-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileC = assembleBundle("bundleC", "/bundles/resolver/simpleimport");

      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            Bundle bundleC = framework.installBundle(fileC);
            try
            {
               // Resolve the installed bundles
               PackageAdmin packageAdmin = getPackageAdmin();
               boolean allResolved = packageAdmin.resolveBundles(null);
               assertTrue("All resolved", allResolved);

               // Verify bundle states
               assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
               assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());
               assertEquals("BundleC RESOLVED", Bundle.RESOLVED, bundleC.getState());

               // Verify that the class load
               assertLoaderBundle(bundleA, bundleA, A.class);
               assertLoaderBundle(bundleB, bundleB, A.class);
               assertLoaderBundle(bundleA, bundleC, A.class);
            }
            finally
            {
               bundleC.uninstall();
            }
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
   public void testPreferredExporterLowerId() throws Exception
   {
      // Bundle-SymbolicName: simpleexport
      // Export-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/simpleexport", A.class);

      // Bundle-SymbolicName: simpleexportother
      // Export-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleexportother", A.class);

      // Bundle-SymbolicName: simpleimport
      // Import-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileC = assembleBundle("bundleC", "/bundles/resolver/simpleimport");

      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertTrue("All resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            Bundle bundleC = framework.installBundle(fileC);
            try
            {
               allResolved = packageAdmin.resolveBundles(null);
               assertTrue("All resolved", allResolved);

               // Verify bundle states
               assertEquals("BundleC RESOLVED", Bundle.RESOLVED, bundleC.getState());

               // Verify that the class load
               assertLoaderBundle(bundleA, bundleA, A.class);
               assertLoaderBundle(bundleB, bundleB, A.class);
               assertLoaderBundle(bundleA, bundleC, A.class);
            }
            finally
            {
               bundleC.uninstall();
            }
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
   public void testPreferredExporterLowerIdReverse() throws Exception
   {
      // Bundle-SymbolicName: simpleexportother
      // Export-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/simpleexportother", A.class);

      // Bundle-SymbolicName: simpleexport
      // Export-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleexport", A.class);

      // Bundle-SymbolicName: simpleimport
      // Import-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileC = assembleBundle("bundleC", "/bundles/resolver/simpleimport");

      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertTrue("All resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            Bundle bundleC = framework.installBundle(fileC);
            try
            {
               allResolved = packageAdmin.resolveBundles(null);
               assertTrue("All resolved", allResolved);

               // Verify bundle states
               assertEquals("BundleC RESOLVED", Bundle.RESOLVED, bundleC.getState());

               // Verify that the class load
               assertLoaderBundle(bundleA, bundleA, A.class);
               assertLoaderBundle(bundleB, bundleB, A.class);
               assertLoaderBundle(bundleA, bundleC, A.class);
            }
            finally
            {
               bundleC.uninstall();
            }
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
   public void testPackageAttribute() throws Exception
   {
      //Bundle-SymbolicName: packageexportattribute
      //Export-Package: org.jboss.test.osgi.classloader.support.a;test=x
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/packageexportattribute", A.class);
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         //Bundle-SymbolicName: simpleimport
         //Import-Package: org.jboss.test.osgi.classloader.support.a
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleimport");
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertTrue("All resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            // Verify that the class load
            assertLoaderBundle(bundleA, bundleA, A.class);
            assertLoaderBundle(bundleA, bundleB, A.class);
         }
         finally
         {
            bundleB.uninstall();
         }

         //Bundle-SymbolicName: packageimportattribute
         //Import-Package: org.jboss.test.osgi.classloader.support.a;test=x
         fileB = assembleBundle("bundleB", "/bundles/resolver/packageimportattribute");
         bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertTrue("All resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            // Verify that the class load
            assertLoaderBundle(bundleA, bundleA, A.class);
            assertLoaderBundle(bundleA, bundleB, A.class);
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
   public void testPackageAttributeFails() throws Exception
   {
      //Bundle-SymbolicName: packageexportattribute
      //Export-Package: org.jboss.test.osgi.classloader.support.a;test=x
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/packageexportattribute", A.class);
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         //Bundle-SymbolicName: packageimportattributefails
         //Import-Package: org.jboss.test.osgi.classloader.support.a;test=y
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/packageimportattributefails");
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertFalse("Not all resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
            assertEquals("BundleB INSTALLED", Bundle.INSTALLED, bundleB.getState());

            // Verify that the class load
            assertLoaderBundle(bundleA, bundleA, A.class);
            assertLoadFails(bundleB, A.class);
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
   public void testPackageAttributeMandatory() throws Exception
   {
      //Bundle-SymbolicName: packageexportattributemandatory
      //Export-Package: org.jboss.test.osgi.classloader.support.a;test=x;mandatory:=test
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/packageexportattributemandatory", A.class);
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         //Bundle-SymbolicName: packageimportattribute
         //Import-Package: org.jboss.test.osgi.classloader.support.a;test=x
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/packageimportattribute");
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertTrue("All resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
            assertEquals("BundleB RESOLVED", Bundle.RESOLVED, bundleB.getState());

            // Verify that the class load
            assertLoaderBundle(bundleA, bundleA, A.class);
            assertLoaderBundle(bundleA, bundleB, A.class);
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
   public void testPackageAttributeMandatoryFails() throws Exception
   {
      //Bundle-SymbolicName: packageexportattributemandatory
      //Export-Package: org.jboss.test.osgi.classloader.support.a;test=x;mandatory:=test
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/packageexportattributemandatory", A.class);
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         //Bundle-SymbolicName: simpleimport
         //Import-Package: org.jboss.test.osgi.classloader.support.a
         VirtualFile fileB = assembleBundle("bundleB", "/bundles/resolver/simpleimport");
         Bundle bundleB = framework.installBundle(fileB);
         try
         {
            // Resolve the installed bundles
            PackageAdmin packageAdmin = getPackageAdmin();
            boolean allResolved = packageAdmin.resolveBundles(null);
            assertFalse("Not all resolved", allResolved);

            // Verify bundle states
            assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
            assertEquals("BundleB INSTALLED", Bundle.INSTALLED, bundleB.getState());

            // Verify that the class load
            assertLoaderBundle(bundleA, bundleA, A.class);
            assertLoadFails(bundleB, A.class);
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
   public void testSystemPackageImport() throws Exception
   {
      //Bundle-SymbolicName: systempackageimport
      //Import-Package: org.osgi.framework;version=1.4
      VirtualFile fileA = assembleBundle("bundleA", "/bundles/resolver/systempackageimport");
      Bundle bundleA = framework.installBundle(fileA);
      try
      {
         // Resolve the installed bundles
         PackageAdmin packageAdmin = getPackageAdmin();
         boolean allResolved = packageAdmin.resolveBundles(null);
         assertTrue("All resolved", allResolved);

         // Verify bundle states
         assertEquals("BundleA RESOLVED", Bundle.RESOLVED, bundleA.getState());
      }
      finally
      {
         bundleA.uninstall();
      }
   }
}