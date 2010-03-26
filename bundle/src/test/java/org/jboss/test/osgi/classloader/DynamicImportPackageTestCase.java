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

import java.io.InputStream;

import org.jboss.osgi.testing.OSGiFrameworkTest;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archives;
import org.jboss.shrinkwrap.api.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

/**
 * Test the DynamicImport-Package manifest header.
 *
 * @author thomas.diesler@jboss.com
 * @since 26-Mar-2010
 */
public class DynamicImportPackageTestCase extends OSGiFrameworkTest
{
   @Test
   public void testLogServiceAvailableOnInstall() throws Exception
   {
      Bundle cmpd = installBundle("bundles/org.osgi.compendium.jar");
      try
      {
         Bundle bundle = installBundle(getBundleArchive());
         assertBundleState(Bundle.INSTALLED, bundle.getState());
         try
         {
            bundle.start();
            assertBundleState(Bundle.ACTIVE, bundle.getState());
            assertLoadClass(bundle, LogService.class.getName());
         }
         finally
         {
            bundle.uninstall();
         }
      }
      finally
      {
         cmpd.uninstall();
      }
   }

   @Test
   public void testLogServiceNotAvailableOnInstall() throws Exception
   {
      Bundle bundle = installBundle(getBundleArchive());
      assertBundleState(Bundle.INSTALLED, bundle.getState());
      try
      {
         bundle.start();
         assertBundleState(Bundle.ACTIVE, bundle.getState());
         assertLoadClassFail(bundle, LogService.class.getName());
         
         Bundle cmpd = installBundle("bundles/org.osgi.compendium.jar");
         try
         {
            System.out.println("FIXME [JBCL-131] Add a notion of on demand resolution");
            //assertLoadClass(bundle, LogService.class.getName());
         }
         finally
         {
            cmpd.uninstall();
         }
      }
      finally
      {
         bundle.uninstall();
      }
   }

   private JavaArchive getBundleArchive()
   {
      final JavaArchive archive = Archives.create("dynamic-log-service", JavaArchive.class);
      archive.setManifest(new Asset()
      {
         public InputStream openStream()
         {
            OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
            builder.addBundleManifestVersion(2);
            builder.addBundleSymbolicName(archive.getName());
            builder.addDynamicImportPackages("org.osgi.service.log");
            return builder.openStream();
         }
      });
      return archive;
   }
}
