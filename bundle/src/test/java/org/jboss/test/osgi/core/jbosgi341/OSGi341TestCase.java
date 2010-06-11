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
package org.jboss.test.osgi.core.jbosgi341;

// $Id: $

import java.io.InputStream;

import org.jboss.osgi.testing.OSGiFrameworkTest;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archives;
import org.jboss.shrinkwrap.api.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.test.osgi.core.jbosgi341.support.a.A;
import org.jboss.test.osgi.core.jbosgi341.support.b.B;
import org.jboss.test.osgi.core.jbosgi341.support.c.C;
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * [JBOSGI-341] Endless loop at AS server startup
 *
 * https://jira.jboss.org/jira/browse/JBOSGI-341
 * 
 * @author thomas.diesler@jboss.com
 * @since 10-Jun-2010
 */
public class OSGi341TestCase extends OSGiFrameworkTest
{
   @Test
   public void testEventAdmin() throws Exception
   {
      Bundle common = installBundle("bundles/jboss-osgi-common.jar");
      Bundle eventadmin = installBundle("bundles/org.apache.felix.eventadmin.jar");
      Bundle cmpd = installBundle("bundles/org.osgi.compendium.jar");
      try
      {
         assertBundleState(Bundle.INSTALLED, common.getState());
         assertBundleState(Bundle.INSTALLED, eventadmin.getState());
         assertBundleState(Bundle.INSTALLED, cmpd.getState());
         
         PackageAdmin pa = getPackageAdmin();
         pa.resolveBundles(null);
         
         assertBundleState(Bundle.RESOLVED, common.getState());
         assertBundleState(Bundle.RESOLVED, eventadmin.getState());
         assertBundleState(Bundle.RESOLVED, cmpd.getState());
      }
      finally
      {
         common.uninstall();
         eventadmin.uninstall();
         cmpd.uninstall();
      }
   }
   
   @Ignore
   public void testCircularMandatory() throws Exception
   {
      Bundle bundleA = installBundle(A.class, B.class, Constants.RESOLUTION_MANDATORY);
      Bundle bundleB = installBundle(B.class, C.class, Constants.RESOLUTION_MANDATORY);
      Bundle bundleC = installBundle(C.class, A.class, Constants.RESOLUTION_MANDATORY);
      
      try
      {
         bundleA.start();
         assertBundleState(Bundle.ACTIVE, bundleA.getState());
         assertBundleState(Bundle.RESOLVED, bundleB.getState());
         assertBundleState(Bundle.RESOLVED, bundleC.getState());
      }
      finally
      {
         bundleA.uninstall();
         bundleB.uninstall();
         bundleC.uninstall();
      }
   }

   @Ignore
   public void testCircularOptional() throws Exception
   {
      Bundle bundleA = installBundle(A.class, B.class, Constants.RESOLUTION_OPTIONAL);
      Bundle bundleB = installBundle(B.class, C.class, Constants.RESOLUTION_OPTIONAL);
      Bundle bundleC = installBundle(C.class, A.class, Constants.RESOLUTION_OPTIONAL);
      
      try
      {
         bundleA.start();
         assertBundleState(Bundle.ACTIVE, bundleA.getState());
         assertBundleState(Bundle.RESOLVED, bundleB.getState());
         assertBundleState(Bundle.RESOLVED, bundleC.getState());
      }
      finally
      {
         bundleA.uninstall();
         bundleB.uninstall();
         bundleC.uninstall();
      }
   }

   private Bundle installBundle(final Class<?> exp, final Class<?> imp, final String resolution) throws Exception
   {
      final JavaArchive archive = Archives.create("jbosgi323-bundle" + exp.getSimpleName(), JavaArchive.class);
      archive.addClass(exp);
      archive.addClass(imp);
      archive.setManifest(new Asset()
      {
         public InputStream openStream()
         {
            OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
            builder.addBundleManifestVersion(2);
            builder.addBundleSymbolicName(archive.getName());
            builder.addExportPackages(exp);
            builder.addImportPackages(imp.getPackage().getName() + ";resolution:=" + resolution);
            return builder.openStream();
         }
      });
      
      return installBundle(archive);
   }
}
