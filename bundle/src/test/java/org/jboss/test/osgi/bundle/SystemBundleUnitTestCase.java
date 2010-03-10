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

// $Id: $

import static org.junit.Assert.*;

import java.util.Dictionary;
import java.util.Hashtable;

import org.jboss.test.osgi.AbstractFrameworkTest;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * SystemBundleUnitTestCase.
 *
 * TODO test security
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class SystemBundleUnitTestCase extends AbstractFrameworkTest
{
   @Test
   public void testBundleId() throws Exception
   {
      assertEquals(0, framework.getBundleId());
   }
   
   @Test
   public void testSymbolicName() throws Exception
   {
      assertEquals(Constants.SYSTEM_BUNDLE_SYMBOLICNAME, framework.getSymbolicName());
   }
   
   @Test
   public void testState() throws Exception
   {
      assertEquals(Bundle.ACTIVE, framework.getState());
   }
   
   @Test
   public void testStartStop() throws Exception
   {
      System.out.println("FIXME [JBOSGI-138] Proper system BundleContext implementation");
   }
   
   @Test
   public void testUpdate() throws Exception
   {
      System.out.println("FIXME [JBOSGI-138] Proper system BundleContext implementation");
   }
   
   @Test
   public void testUninstall() throws Exception
   {
      try
      {
         framework.uninstall();
         fail("Should not be here!");
      }
      catch (BundleException t)
      {
         // expected
      }
   }
   
   @Test
   @SuppressWarnings({ "unchecked", "rawtypes" })
   public void testGetHeaders() throws Exception
   {
      Dictionary expected = new Hashtable();
      expected.put(Constants.BUNDLE_SYMBOLICNAME, Constants.SYSTEM_BUNDLE_SYMBOLICNAME);
      // todo expected.put(Attributes.Name.IMPLEMENTATION_TITLE.toString(), "JBoss OSGi");
      // todo expected.put(Attributes.Name.IMPLEMENTATION_VENDOR.toString(), "jboss.org");
      // todo expected.put(Attributes.Name.IMPLEMENTATION_VERSION.toString(), "r4v41");
      
      Dictionary dictionary = framework.getHeaders();
      assertEquals(expected, dictionary);
   }
   
   @Test
   public void testLocation() throws Exception
   {
      assertEquals(Constants.SYSTEM_BUNDLE_LOCATION, framework.getLocation());
   }
   
   @Test
   public void testGetEntry()
   {
      System.out.println("FIXME [JBOSGI-138] Proper system BundleContext implementation");
   }
   
   @Test
   public void testGetEntryPath()
   {
      System.out.println("FIXME [JBOSGI-138] Proper system BundleContext implementation");
   }
   
   @Test
   public void testFindEntries()
   {
      System.out.println("FIXME [JBOSGI-138] Proper system BundleContext implementation");
   }
   
   @Test
   public void testLoadClass()
   {
      System.out.println("FIXME [JBOSGI-138] Proper system BundleContext implementation");
   }
   
   @Test
   public void testGetResource()
   {
      System.out.println("FIXME [JBOSGI-138] Proper system BundleContext implementation");
   }
   
   @Test
   public void testGetResources()
   {
      System.out.println("FIXME [JBOSGI-138] Proper system BundleContext implementation");
   }
}
