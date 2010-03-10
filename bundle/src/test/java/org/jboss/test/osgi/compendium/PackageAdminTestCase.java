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

// $Id: $

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.jboss.osgi.vfs.VirtualFile;
import org.jboss.test.osgi.NativeFrameworkTest;
import org.jboss.test.osgi.compendium.support.a.PA;
import org.jboss.test.osgi.compendium.support.b.Other;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * Test PackageAdmin service.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author thomas.diesler@jboss.com
 */
public class PackageAdminTestCase extends NativeFrameworkTest
{
   @Test
   public void testGetBudleFromClass() throws Exception
   {
      VirtualFile assemblyA = assembleBundle("smoke-assembled", "/bundles/smoke/smoke-assembled", PA.class);
      Bundle bundleA = context.installBundle(assemblyA.toURL().toExternalForm());
      try
      {
         bundleA.start();
         Class<?> paClass = assertLoadClass(bundleA, PA.class.getName());

         PackageAdmin pa = getPackageAdmin();

         Bundle found = pa.getBundle(paClass);
         assertSame(bundleA, found);

         Bundle notFound = pa.getBundle(getClass());
         assertNull(notFound);

         VirtualFile assemblyB = assembleBundle("simple", "/bundles/simple/simple-bundle1", Other.class);
         Bundle bundleB = context.installBundle(assemblyB.toURL().toExternalForm());
         try
         {
            bundleB.start();
            Class<?> otherClass = assertLoadClass(bundleB, Other.class.getName());

            found = pa.getBundle(otherClass);
            assertSame(bundleB, found);
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
