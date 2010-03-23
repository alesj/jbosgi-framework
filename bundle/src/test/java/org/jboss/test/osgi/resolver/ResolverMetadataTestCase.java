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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.jboss.osgi.framework.resolver.ExportPackage;
import org.jboss.osgi.framework.resolver.ImportPackage;
import org.jboss.osgi.framework.resolver.RequiredBundle;
import org.jboss.osgi.framework.resolver.Resolver;
import org.jboss.osgi.framework.resolver.ResolverBundle;
import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.test.osgi.classloader.support.a.A;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Test {@link Resolver} metadata.
 * 
 * @author thomas.diesler@jboss.com
 * @since 09-Nov-2009
 */
public class ResolverMetadataTestCase extends AbstractResolverTest
{
   @Test
   public void testSimpleExport() throws Exception
   {
      Resolver resolver = getTestResolver();
      if (resolver == null)
         return;

      // Bundle-SymbolicName: simpleexport
      // Export-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileA = assembleArchive("bundleA", "/bundles/resolver/simpleexport", A.class);
      Bundle bundleA = installBundle(fileA);
      try
      {
         assertEquals(1, resolver.getBundles().size());

         ResolverBundle resBundleA = resolver.getBundle(bundleA.getSymbolicName(), null);
         assertNotNull("Resolvable not null", resBundleA);

         resBundleA = resolver.getBundle(bundleA.getSymbolicName(), bundleA.getVersion());
         assertNotNull("Resolvable not null", resBundleA);

         resBundleA = resolver.getBundle(bundleA);
         assertNotNull("Resolvable not null", resBundleA);

         assertNotNull(resBundleA.getBundle());
         assertEquals("simpleexport", resBundleA.getSymbolicName());
         assertEquals(Version.emptyVersion, resBundleA.getVersion());

         List<ExportPackage> exportPackages = resBundleA.getExportPackages();
         assertNotNull("ExportPackages not null", exportPackages);
         assertEquals(1, exportPackages.size());

         ExportPackage exportPackage = resBundleA.getExportPackage("org.jboss.test.osgi.classloader.support.a");
         assertNotNull("ExportPackage not null", exportPackage);
         assertEquals("org.jboss.test.osgi.classloader.support.a", exportPackage.getName());

         assertEquals(Version.emptyVersion, exportPackage.getVersion());
         assertEquals(0, exportPackage.getUses().size());
         assertEquals(0, exportPackage.getMandatory().size());
         assertNull("Null includes", exportPackage.getIncludes());
         assertNull("Null excludes", exportPackage.getExcludes());

         List<ImportPackage> importPackages = resBundleA.getImportPackages();
         assertNotNull("ImportPackages not null", importPackages);
         assertEquals(0, importPackages.size());

         assertFalse("No sigleton", resBundleA.isSingleton());
         assertFalse("Not resolved", resBundleA.isResolved());
      }
      finally
      {
         bundleA.uninstall();
      }
   }

   @Test
   public void testSimpleImport() throws Exception
   {
      Resolver resolver = getTestResolver();
      if (resolver == null)
         return;

      // Bundle-SymbolicName: simpleimport
      // Import-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileA = assembleArchive("bundleA", "/bundles/resolver/simpleimport");
      Bundle bundleA = installBundle(fileA);
      try
      {
         ResolverBundle resBundleA = resolver.getBundle(bundleA);

         List<ImportPackage> importPackages = resBundleA.getImportPackages();
         assertNotNull("ImportPackages not null", importPackages);
         assertEquals(1, importPackages.size());

         ImportPackage importPackage = resBundleA.getImportPackage("org.jboss.test.osgi.classloader.support.a");
         assertNotNull("ImportPackage not null", importPackage);
         assertEquals("org.jboss.test.osgi.classloader.support.a", importPackage.getName());

         assertEquals("[0.0.0,?)", importPackage.getVersion().toString());
         assertNull("Null bundle-symbolic-name", importPackage.getBundleSymbolicName());
         assertNull("Null bundle-version", importPackage.getBundleVersion());
      }
      finally
      {
         bundleA.uninstall();
      }
   }

   @Test
   public void testSingleton() throws Exception
   {
      Resolver resolver = getTestResolver();
      if (resolver == null)
         return;

      // Bundle-SymbolicName: singleton;singleton:=true
      // Export-Package: org.jboss.test.osgi.classloader.support.a
      VirtualFile fileA = assembleArchive("bundleA", "/bundles/resolver/singleton", A.class);
      Bundle bundleA = installBundle(fileA);
      try
      {
         ResolverBundle resBundleA = resolver.getBundle(bundleA);
         assertTrue("Sigleton", resBundleA.isSingleton());
      }
      finally
      {
         bundleA.uninstall();
      }
   }

   @Test
   public void testRequireBundle() throws Exception
   {
      Resolver resolver = getTestResolver();
      if (resolver == null)
         return;

      //Bundle-SymbolicName: requirebundle
      //Require-Bundle: simpleexport
      VirtualFile fileA = assembleArchive("bundleA", "/bundles/resolver/requirebundle");
      Bundle bundleA = installBundle(fileA);
      try
      {
         ResolverBundle resBundleA = resolver.getBundle(bundleA);
         List<RequiredBundle> requiredBundles = resBundleA.getRequiredBundles();
         assertEquals("RequiredBundles not null", 1, requiredBundles.size());
         RequiredBundle reqBundle = requiredBundles.get(0);
         assertEquals("simpleexport", reqBundle.getSymbolicName());
         assertNull("Null version", reqBundle.getVersion());
         assertFalse("Not optional", reqBundle.isOptional());
      }
      finally
      {
         bundleA.uninstall();
      }
   }

   @Test
   public void testRequireBundleOptional() throws Exception
   {
      Resolver resolver = getTestResolver();
      if (resolver == null)
         return;

      //Bundle-SymbolicName: requirebundle
      //Require-Bundle: simpleexport;resolution:=optional
      VirtualFile fileA = assembleArchive("bundleA", "/bundles/resolver/requirebundleoptional");
      Bundle bundleA = installBundle(fileA);
      try
      {
         ResolverBundle resBundleA = resolver.getBundle(bundleA);
         List<RequiredBundle> requiredBundles = resBundleA.getRequiredBundles();
         assertEquals("RequiredBundles not null", 1, requiredBundles.size());
         RequiredBundle reqBundle = requiredBundles.get(0);
         assertEquals("simpleexport", reqBundle.getSymbolicName());
         assertNull("Null version", reqBundle.getVersion());
         assertTrue("Resolution optional", reqBundle.isOptional());
      }
      finally
      {
         bundleA.uninstall();
      }
   }

   @Test
   public void testRequireBundleVersion() throws Exception
   {
      Resolver resolver = getTestResolver();
      if (resolver == null)
         return;

      //Bundle-SymbolicName: requirebundle
      //Require-Bundle: simpleexport;bundle-version="[0.0.0,1.0.0]"
      VirtualFile fileA = assembleArchive("bundleA", "/bundles/resolver/requirebundleversion");
      Bundle bundleA = installBundle(fileA);
      try
      {
         ResolverBundle resBundleA = resolver.getBundle(bundleA);
         List<RequiredBundle> requiredBundles = resBundleA.getRequiredBundles();
         assertEquals("RequiredBundles not null", 1, requiredBundles.size());
         RequiredBundle reqBundle = requiredBundles.get(0);
         assertEquals("simpleexport", reqBundle.getSymbolicName());
         assertNotNull("Version not null", reqBundle.getVersion());
         assertFalse("Not optional", reqBundle.isOptional());
      }
      finally
      {
         bundleA.uninstall();
      }
   }

   @Test
   public void testPackageAttribute() throws Exception
   {
      Resolver resolver = getTestResolver();
      if (resolver == null)
         return;

      //Bundle-SymbolicName: packageexportattribute
      //Export-Package: org.jboss.test.osgi.classloader.support.a;test=x
      VirtualFile fileA = assembleArchive("bundleA", "/bundles/resolver/packageexportattribute");
      Bundle bundleA = installBundle(fileA);
      try
      {
         ResolverBundle resBundleA = resolver.getBundle(bundleA);
         ExportPackage exportPackage = resBundleA.getExportPackage("org.jboss.test.osgi.classloader.support.a");
         Set<String> exportAttributes = exportPackage.getAttributes();
         assertTrue("Contains attr", exportAttributes.contains("test"));
         assertEquals("x", exportPackage.getAttribute("test"));

         //Bundle-SymbolicName: simpleimport
         //Import-Package: org.jboss.test.osgi.classloader.support.a
         VirtualFile fileB = assembleArchive("bundleB", "/bundles/resolver/simpleimport");
         Bundle bundleB = installBundle(fileB);
         try
         {
            ResolverBundle resBundleB = resolver.getBundle(bundleB);
            ImportPackage importPackage = resBundleB.getImportPackage("org.jboss.test.osgi.classloader.support.a");
            assertTrue("Attribute match", exportPackage.matchAttributes(importPackage));
         }
         finally
         {
            bundleB.uninstall();
         }

         //Bundle-SymbolicName: packageimportattribute
         //Import-Package: org.jboss.test.osgi.classloader.support.a;test=x
         fileB = assembleArchive("bundleB", "/bundles/resolver/packageimportattribute");
         bundleB = installBundle(fileB);
         try
         {
            ResolverBundle resBundleB = resolver.getBundle(bundleB);
            ImportPackage importPackage = resBundleB.getImportPackage("org.jboss.test.osgi.classloader.support.a");
            Set<String> importAttributes = importPackage.getAttributes();
            assertTrue("Contains attr", importAttributes.contains("test"));
            assertEquals("x", importPackage.getAttribute("test"));
            assertTrue("Attribute match", exportPackage.matchAttributes(importPackage));
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
      Resolver resolver = getTestResolver();
      if (resolver == null)
         return;

      //Bundle-SymbolicName: packageexportattribute
      //Export-Package: org.jboss.test.osgi.classloader.support.a;test=x
      VirtualFile fileA = assembleArchive("bundleA", "/bundles/resolver/packageexportattribute");
      Bundle bundleA = installBundle(fileA);
      try
      {
         ResolverBundle resBundleA = resolver.getBundle(bundleA);
         ExportPackage exportPackage = resBundleA.getExportPackage("org.jboss.test.osgi.classloader.support.a");
         Set<String> attributes = exportPackage.getAttributes();
         assertTrue("Contains attr", attributes.contains("test"));
         assertEquals("x", exportPackage.getAttribute("test"));

         //Bundle-SymbolicName: packageimportattributefails
         //Import-Package: org.jboss.test.osgi.classloader.support.a;test=y
         VirtualFile fileB = assembleArchive("bundleB", "/bundles/resolver/packageimportattributefails");
         Bundle bundleB = installBundle(fileB);
         try
         {
            ResolverBundle resBundleB = resolver.getBundle(bundleB);
            ImportPackage importPackage = resBundleB.getImportPackage("org.jboss.test.osgi.classloader.support.a");
            Set<String> importAttributes = importPackage.getAttributes();
            assertTrue("Contains attr", importAttributes.contains("test"));
            assertEquals("y", importPackage.getAttribute("test"));
            assertFalse("Attribute no match", exportPackage.matchAttributes(importPackage));
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
      Resolver resolver = getTestResolver();
      if (resolver == null)
         return;

      //Bundle-SymbolicName: packageexportattributemandatory
      //Export-Package: org.jboss.test.osgi.classloader.support.a;test=x;mandatory:=test
      VirtualFile fileA = assembleArchive("bundleA", "/bundles/resolver/packageexportattributemandatory");
      Bundle bundleA = installBundle(fileA);
      try
      {
         ResolverBundle resBundleA = resolver.getBundle(bundleA);
         ExportPackage exportPackage = resBundleA.getExportPackage("org.jboss.test.osgi.classloader.support.a");
         Set<String> attributes = exportPackage.getAttributes();
         assertTrue("Contains test", attributes.contains("test"));
         assertEquals("x", exportPackage.getAttribute("test"));
         Set<String> mandatory = exportPackage.getMandatory();
         assertTrue("Contains test", mandatory.contains("test"));

         //Bundle-SymbolicName: packageimportattribute
         //Import-Package: org.jboss.test.osgi.classloader.support.a;test=x
         VirtualFile fileB = assembleArchive("bundleB", "/bundles/resolver/packageimportattribute");
         Bundle bundleB = installBundle(fileB);
         try
         {
            ResolverBundle resBundleB = resolver.getBundle(bundleB);
            ImportPackage importPackage = resBundleB.getImportPackage("org.jboss.test.osgi.classloader.support.a");
            Set<String> importAttributes = importPackage.getAttributes();
            assertTrue("Contains attr", importAttributes.contains("test"));
            assertEquals("x", importPackage.getAttribute("test"));
            assertTrue("Attribute match", exportPackage.matchAttributes(importPackage));
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
      Resolver resolver = getTestResolver();
      if (resolver == null)
         return;

      //Bundle-SymbolicName: packageexportattributemandatory
      //Export-Package: org.jboss.test.osgi.classloader.support.a;test=x;mandatory:=test
      VirtualFile fileA = assembleArchive("bundleA", "/bundles/resolver/packageexportattributemandatory");
      Bundle bundleA = installBundle(fileA);
      try
      {
         ResolverBundle resBundleA = resolver.getBundle(bundleA);
         ExportPackage exportPackage = resBundleA.getExportPackage("org.jboss.test.osgi.classloader.support.a");
         Set<String> attributes = exportPackage.getAttributes();
         assertTrue("Contains test", attributes.contains("test"));
         assertEquals("x", exportPackage.getAttribute("test"));
         Set<String> mandatory = exportPackage.getMandatory();
         assertTrue("Contains test", mandatory.contains("test"));

         //Bundle-SymbolicName: simpleimport
         //Import-Package: org.jboss.test.osgi.classloader.support.a
         VirtualFile fileB = assembleArchive("bundleB", "/bundles/resolver/simpleimport");
         Bundle bundleB = installBundle(fileB);
         try
         {
            ResolverBundle resBundleB = resolver.getBundle(bundleB);
            ImportPackage importPackage = resBundleB.getImportPackage("org.jboss.test.osgi.classloader.support.a");
            assertFalse("Attribute no match", exportPackage.matchAttributes(importPackage));
         }
         finally
         {
            bundleB.uninstall();
         }

         //Bundle-SymbolicName: packageimportattributefails
         //Import-Package: org.jboss.test.osgi.classloader.support.a;test=y
         fileB = assembleArchive("bundleB", "/bundles/resolver/packageimportattributefails");
         bundleB = installBundle(fileB);
         try
         {
            ResolverBundle resBundleB = resolver.getBundle(bundleB);
            ImportPackage importPackage = resBundleB.getImportPackage("org.jboss.test.osgi.classloader.support.a");
            Set<String> importAttributes = importPackage.getAttributes();
            assertTrue("Contains attr", importAttributes.contains("test"));
            assertEquals("y", importPackage.getAttribute("test"));
            assertFalse("Attribute no match", exportPackage.matchAttributes(importPackage));
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
}