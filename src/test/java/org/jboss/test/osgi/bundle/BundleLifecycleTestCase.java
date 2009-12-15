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
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

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

   public void testExceptionOnStart() throws Exception
   {
      Bundle bundle = assembleBundle("fail-on-start", "/bundles/lifecycle/fail-on-start", FailOnStartActivator.class);
      try
      {
         assertBundleState(Bundle.INSTALLED, bundle.getState());

         try
         {
            bundle.start();
            fail("BundleException expected");
         }
         catch (BundleException ex)
         {
            assertBundleState(Bundle.RESOLVED, bundle.getState());
         }
      }
      finally
      {
         //bundle.uninstall();
         //assertBundleState(Bundle.UNINSTALLED, bundle.getState());
      }
   }
}
