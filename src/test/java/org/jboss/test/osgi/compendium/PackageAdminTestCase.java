/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.test.osgi.compendium;

import junit.framework.Test;

import org.jboss.osgi.framework.service.internal.PackageAdminImpl;
import org.jboss.test.osgi.FrameworkTest;
import org.jboss.test.osgi.compendium.support.b.Other;
import org.jboss.test.osgi.compendium.support.a.PA;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Test PackageAdmin service.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class PackageAdminTestCase extends FrameworkTest
{
   public PackageAdminTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(PackageAdminTestCase.class);
   }

   protected PackageAdmin createPackageAdmin()
   {
      return new PackageAdminImpl(getBundleManager());
   }

   public void testGetBudleFromClass() throws Exception
   {
      Bundle bundle = assembleBundle("smoke-assembled", "/bundles/smoke/smoke-assembled", PA.class);
      try
      {
         bundle.start();
         Class<?> paClass = assertLoadClass(bundle, PA.class);

         PackageAdmin pa = createPackageAdmin();

         Bundle found = pa.getBundle(paClass);
         assertSame(bundle, found);

         Bundle notFound = pa.getBundle(getClass());
         assertNull(notFound);

         Bundle other = assembleBundle("simple", "/bundles/simple/simple-bundle1", Other.class);
         try
         {
            other.start();
            Class<?> otherClass = assertLoadClass(other, Other.class);

            found = pa.getBundle(otherClass);
            assertSame(other, found);
         }
         finally
         {
            other.uninstall();
         }
      }
      finally
      {
         bundle.uninstall();
      }
   }
}
