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
package org.jboss.test.osgi.core.bootstrap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.jboss.osgi.framework.launch.OSGiFrameworkFactory;
import org.jboss.osgi.testing.OSGiTest;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;

/**
 * Test various bootstrap options.
 * 
 * @author thomas.diesler@jboss.com
 * @since 29-Apr-2010
 */
public class FrameworkBootstrapTestCase extends OSGiTest
{
   @Test
   public void testBootstrapURL() throws BundleException
   {
      try
      {
         String bootURL = getResourceURL("bootstrap/test-bootstrap-one.xml").toString();
         System.setProperty(OSGiFrameworkFactory.BOOTSTRAP_URLS, bootURL);
         Framework framework = new OSGiFrameworkFactory().newFramework(null);
         assertNotNull("Framework not null", framework);

         try
         {
            System.setProperty(OSGiFrameworkFactory.BOOTSTRAP_URLS, "file:///does-not-exist");
            new OSGiFrameworkFactory().newFramework(null);
            fail("IllegalStateException expected");
         }
         catch (IllegalStateException ex)
         {
            // expected
         }
      }
      finally
      {
         System.clearProperty(OSGiFrameworkFactory.BOOTSTRAP_URLS);
      }
   }

   @Test
   public void testBootstrapPath() throws BundleException
   {
      try
      {
         System.setProperty(OSGiFrameworkFactory.BOOTSTRAP_PATHS, "bootstrap/META-INF/test-bootstrap-two.xml");
         Framework framework = new OSGiFrameworkFactory().newFramework(null);
         assertNotNull("Framework not null", framework);
         
         try
         {
            System.setProperty(OSGiFrameworkFactory.BOOTSTRAP_PATHS, "does-not-exist");
            new OSGiFrameworkFactory().newFramework(null);
            fail("IllegalStateException expected");
         }
         catch (IllegalStateException ex)
         {
            // expected
         }
      }
      finally
      {
         System.clearProperty(OSGiFrameworkFactory.BOOTSTRAP_PATHS);
      }
   }
}