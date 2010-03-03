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

import junit.framework.Test;

import org.jboss.test.osgi.FrameworkTest;
import org.jboss.test.osgi.classloader.support.a.A;
import org.jboss.test.osgi.classloader.support.b.B;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * RequireBundleUnitTestCase.
 *
 * TODO test security
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class RequireBundleUnitTestCase extends FrameworkTest
{
   public RequireBundleUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(RequireBundleUnitTestCase.class);
   }

   public void testSimpleRequireBundle() throws Exception
   {
      Bundle bundle1 = installBundle(assembleBundle("bundleA", "/bundles/classloader/bundleA", A.class));
      try
      {
         bundle1.start();
         assertLoadClass(bundle1, A.class);
         Bundle bundle2 = installBundle(assembleBundle("simplerequirebundleA", "/bundles/classloader/simplerequirebundleA", B.class));
         try
         {
            bundle2.start();
            assertLoadClass(bundle2, A.class, bundle1);
            assertLoadClass(bundle2, B.class, bundle2);
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
   
   public void testSimpleRequireBundleFails() throws Exception
   {
      Bundle bundle1 = installBundle(assembleBundle("bundleA", "/bundles/classloader/bundleA", A.class));
      try
      {
         bundle1.start();
         assertLoadClass(bundle1, A.class);
         Bundle bundle2 = installBundle(assembleBundle("simplerequirebundlefails", "/bundles/classloader/simplerequirebundlefails", B.class));
         try
         {
            bundle2.start();
            fail("Should not be here!");
         }
         catch (BundleException ex)
         {
            // expected
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
   
   public void testVersionRequireBundle() throws Exception
   {
      Bundle bundle1 = installBundle(assembleBundle("bundleA", "/bundles/classloader/bundleA", A.class));
      try
      {
         bundle1.start();
         assertLoadClass(bundle1, A.class);
         Bundle bundle2 = installBundle(assembleBundle("versionrequirebundleA", "/bundles/classloader/versionrequirebundleA", B.class));
         try
         {
            bundle2.start();
            assertLoadClass(bundle2, A.class, bundle1);
            assertLoadClass(bundle2, B.class, bundle2);
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
   
   public void testVersionRequireBundleFails() throws Exception
   {
      Bundle bundle1 = installBundle(assembleBundle("bundleA", "/bundles/classloader/bundleA", A.class));
      try
      {
         bundle1.start();
         assertLoadClass(bundle1, A.class);
         Bundle bundle2 = installBundle(assembleBundle("versionrequirebundlefails", "/bundles/classloader/versionrequirebundlefails", B.class));
         try
         {
            bundle2.start();
            fail("Should not be here!");
         }
         catch (BundleException rte)
         {
            // expected
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
   
   public void testOptionalRequireBundle() throws Exception
   {
      Bundle bundle1 = installBundle(assembleBundle("bundleA", "/bundles/classloader/bundleA", A.class));
      try
      {
         bundle1.start();
         assertLoadClass(bundle1, A.class);
         Bundle bundle2 = installBundle(assembleBundle("optionalrequirebundleA", "/bundles/classloader/optionalrequirebundleA", B.class));
         try
         {
            bundle2.start();
            assertLoadClass(bundle2, A.class, bundle1);
            assertLoadClass(bundle2, B.class, bundle2);
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
   
   public void testOptionalRequireBundleFails() throws Exception
   {
      Bundle bundle1 = installBundle(assembleBundle("bundleA", "/bundles/classloader/bundleA", A.class));
      try
      {
         bundle1.start();
         assertLoadClass(bundle1, A.class);
         Bundle bundle2 = installBundle(assembleBundle("optionalrequirebundlefails", "/bundles/classloader/optionalrequirebundlefails", B.class));
         try
         {
            bundle2.start();
            assertLoadClassFail(bundle2, A.class);
            assertLoadClass(bundle2, B.class);
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
   
   public void testReExportRequireBundle() throws Exception
   {
      //Bundle-Name: BundleA
      //Bundle-Version: 1.0.0
      //Bundle-SymbolicName: org.jboss.test.osgi.classloader.bundleA;test=x
      //Export-Package: org.jboss.test.osgi.classloader.support.a;version=1.0.0;test=x
      Bundle bundle1 = installBundle(assembleBundle("bundleA", "/bundles/classloader/bundleA", A.class));
      
      try
      {
         bundle1.start();
         assertLoadClass(bundle1, A.class);
         
         //Bundle-Name: BundleB
         //Bundle-SymbolicName: org.jboss.test.osgi.classloader.bundleB
         //Require-Bundle: org.jboss.test.osgi.classloader.bundleA;visibility:=reexport
         //Export-Package: org.jboss.test.osgi.classloader.support.b
         Bundle bundle2 = installBundle(assembleBundle("reexportrequirebundleA", "/bundles/classloader/reexportrequirebundleA", B.class));
         
         try
         {
            bundle2.start();
            assertLoadClass(bundle2, A.class, bundle1);
            assertLoadClass(bundle2, B.class, bundle2);
            
            //Bundle-Name: BundleC
            //Bundle-SymbolicName: org.jboss.test.osgi.classloader.bundleC
            //Require-Bundle: org.jboss.test.osgi.classloader.bundleB
            Bundle bundle3 = installBundle(assembleBundle("reexportrequirebundleB", "/bundles/classloader/reexportrequirebundleB"));
            
            try
            {
               assertLoadClass(bundle3, A.class, bundle1);
               assertLoadClass(bundle3, B.class, bundle2);
            }
            finally
            {
               uninstall(bundle3);
            }
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
   
   public void testNoReExportRequireBundle() throws Exception
   {
      Bundle bundle1 = installBundle(assembleBundle("bundleA", "/bundles/classloader/bundleA", A.class));
      try
      {
         bundle1.start();
         assertLoadClass(bundle1, A.class);
         Bundle bundle2 = installBundle(assembleBundle("noreexportrequirebundleA", "/bundles/classloader/noreexportrequirebundleA", B.class));
         try
         {
            bundle2.start();
            assertLoadClass(bundle2, A.class, bundle1);
            assertLoadClass(bundle2, B.class, bundle2);
            Bundle bundle3 = installBundle(assembleBundle("reexportrequirebundleB", "/bundles/classloader/reexportrequirebundleB"));
            try
            {
               assertLoadClassFail(bundle3, A.class);
               assertLoadClass(bundle3, B.class, bundle2);
            }
            finally
            {
               uninstall(bundle3);
            }
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
   
   public void testAttributeRequireBundle() throws Exception
   {
      Bundle bundle1 = installBundle(assembleBundle("bundleA", "/bundles/classloader/bundleA", A.class));
      try
      {
         bundle1.start();
         assertLoadClass(bundle1, A.class);
         Bundle bundle2 = installBundle(assembleBundle("attributerequirebundleA", "/bundles/classloader/attributerequirebundleA", B.class));
         try
         {
            bundle2.start();
            assertLoadClass(bundle2, A.class, bundle1);
            assertLoadClass(bundle2, B.class, bundle2);
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
   
   public void testAttributeRequireBundleFails() throws Exception
   {
      // Bundle-SymbolicName: org.jboss.test.osgi.classloader.bundleA;test=x
      // Export-Package: org.jboss.test.osgi.classloader.support.a;version=1.0.0;test=x
      // Bundle-Version: 1.0.0
      Bundle bundle1 = installBundle(assembleBundle("bundleA", "/bundles/classloader/bundleA", A.class));
      try
      {
         bundle1.start();
         assertLoadClass(bundle1, A.class);
         
         // Bundle-SymbolicName: org.jboss.test.osgi.classloader.bundleB
         // Require-Bundle: org.jboss.test.osgi.classloader.bundleA;doesnotexist=true;test=y
         Bundle bundle2 = installBundle(assembleBundle("attributerequirebundlefails", "/bundles/classloader/attributerequirebundlefails", B.class));
         try
         {
            bundle2.start();
            fail("Should not be here!");
         }
         catch (BundleException rte)
         {
            // expected
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
}
