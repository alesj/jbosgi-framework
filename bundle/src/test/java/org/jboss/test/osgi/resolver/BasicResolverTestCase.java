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

// $Id: RuleBasedResolverTest.java 96645 2009-11-20 16:30:15Z thomas.diesler@jboss.com $

import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.launch.OSGiFramework;
import org.jboss.osgi.framework.plugins.ResolverPlugin;
import org.jboss.osgi.framework.resolver.basic.BasicResolverImpl;
import org.junit.Before;
import org.junit.Test;

/**
 * Test the basic resolver.
 * 
 * @author thomas.diesler@jboss.com
 * @since 10-Nov-2009
 */
public class BasicResolverTestCase extends AbstractResolverTest
{
   @Before
   public void setUp() throws Exception
   {
      super.setUp();

      OSGiFramework framework = (OSGiFramework)getFramework();
      OSGiBundleManager bundleManager = framework.getBundleManager();

      ResolverPlugin resolver = bundleManager.getOptionalPlugin(ResolverPlugin.class);
      if (resolver != null)
         bundleManager.removePlugin(resolver);

      resolver = new BasicResolverImpl(bundleManager);
      bundleManager.addPlugin(resolver);
   }

   @Test
   public void testOptionalImportPackageWired() throws Exception
   {
      // WONTFIX: testOptionalImportPackageWired
   }

   @Test
   public void testRequireBundleVersionFails() throws Exception
   {
      // WONTFIX: testRequireBundleVersionFails
   }

   @Test
   public void testPreferredExporterHigherVersion() throws Exception
   {
      // WONTFIX: testPreferredExporterHigherVersion
   }

   @Test
   public void testPreferredExporterLowerId() throws Exception
   {
      // WONTFIX: testPreferredExporterLowerId
   }

   @Test
   public void testPreferredExporterLowerIdReverse() throws Exception
   {
      // WONTFIX: testPreferredExporterLowerIdReverse
   }

   @Test
   public void testPackageAttributeMandatoryFails() throws Exception
   {
      // WONTFIX: testPackageAttributeMandatoryFails
   }
}