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

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.Test;

import org.jboss.test.osgi.FrameworkTest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * SystemBundleUnitTestCase.
 *
 * TODO test security
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class SystemBundleUnitTestCase extends FrameworkTest
{
   public SystemBundleUnitTestCase(String name)
   {
      super(name);
   }

   public static Test suite()
   {
      return suite(SystemBundleUnitTestCase.class);
   }

   public void testBundleId() throws Exception
   {
      assertEquals(0, getSystemBundle().getBundleId());
   }
   
   public void testSymbolicName() throws Exception
   {
      assertEquals(Constants.SYSTEM_BUNDLE_SYMBOLICNAME, getSystemBundle().getSymbolicName());
   }
   
   public void testState() throws Exception
   {
      assertEquals(Bundle.ACTIVE, getSystemBundle().getState());
   }
   
   public void testStartStop() throws Exception
   {
      // TODO testStartStop
   }
   
   public void testUpdate() throws Exception
   {
      // TODO testUpdate
   }
   
   public void testUninstall() throws Exception
   {
      try
      {
         getSystemBundle().uninstall();
         fail("Should not be here!");
      }
      catch (Throwable t)
      {
         checkThrowable(BundleException.class, t);
      }
   }
   
   @SuppressWarnings({ "unchecked", "rawtypes" })
   public void testGetHeaders() throws Exception
   {
      Dictionary expected = new Hashtable();
      expected.put(Constants.BUNDLE_SYMBOLICNAME, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
      // todo expected.put(Attributes.Name.IMPLEMENTATION_TITLE.toString(), "JBoss OSGi");
      // todo expected.put(Attributes.Name.IMPLEMENTATION_VENDOR.toString(), "jboss.org");
      // todo expected.put(Attributes.Name.IMPLEMENTATION_VERSION.toString(), "r4v41");
      
      Dictionary dictionary = getSystemBundle().getHeaders();
      assertEquals(expected, dictionary);
   }
   
   public void testLocation() throws Exception
   {
      assertEquals(Constants.SYSTEM_BUNDLE_LOCATION, getSystemBundle().getLocation());
   }
   
   public void testGetEntry()
   {
      // TODO [JBOSGI-138] Proper system BundleContext implementation
   }
   
   public void testGetEntryPath()
   {
      // TODO [JBOSGI-138] Proper system BundleContext implementation
   }
   
   public void testFindEntries()
   {
      // TODO [JBOSGI-138] Proper system BundleContext implementation
   }
   
   public void testLoadClass()
   {
      // TODO [JBOSGI-138] Proper system BundleContext implementation
   }
   
   public void testGetResource()
   {
      // TODO [JBOSGI-138] Proper system BundleContext implementation
   }
   
   public void testGetResources()
   {
      // TODO [JBOSGI-138] Proper system BundleContext implementation
   }
}
