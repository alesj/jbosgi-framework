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
package org.jboss.test.osgi.core.jbosgi326;

// $Id: $

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Arrays;

import org.jboss.deployers.client.spi.Deployment;
import org.jboss.osgi.framework.testing.AbstractFrameworkTest;
import org.jboss.osgi.testing.OSGiManifestBuilder;
import org.jboss.shrinkwrap.api.Archives;
import org.jboss.shrinkwrap.api.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * MC service does not maintain properties
 *
 * https://jira.jboss.org/jira/browse/JBOSGI-326
 * 
 * @author thomas.diesler@jboss.com
 * @since 07-May-2010
 */
public class OSGi326TestCase extends AbstractFrameworkTest
{
   @Test
   public void testServiceReferenceBean() throws Exception
   {
      BundleContext sysContext = getFramework().getBundleContext();

      Deployment beanB = createDeployment("beanB", null, SomeServiceB.class);
      deploy(addBeanMetaData(beanB, SomeServiceB.class));
      try
      {
         ServiceReference sref = sysContext.getServiceReference(SomeService.class.getName());
         assertNotNull("ServiceReference not null", sref);

         Long serviceId = (Long)sref.getProperty(Constants.SERVICE_ID);
         assertNotNull("service.id not null", serviceId);
         Long secondId = (Long)sysContext.getServiceReference(SomeService.class.getName()).getProperty(Constants.SERVICE_ID);
         assertEquals(serviceId, secondId);

         String[] objectClass = (String[])sref.getProperty(Constants.OBJECTCLASS);
         assertNotNull("objectClass not null", objectClass);
         assertEquals("objectClass length", 2, objectClass.length);
         assertTrue("objectClass contains SomeService", Arrays.asList(objectClass).contains(SomeService.class.getName()));
         assertTrue("objectClass contains SomeServiceB", Arrays.asList(objectClass).contains(SomeServiceB.class.getName()));

         Bundle bundle = sref.getBundle();
         assertNotNull("Bundle not null", bundle);
      }
      finally
      {
         undeploy(beanB);
      }

      // Verify not getting the ServiceReference after undeploy
      ServiceReference sref = sysContext.getServiceReference(SomeService.class.getName());
      assertNull("ServiceReference null", sref);
   }

   @Test
   public void testServiceReferenceBundle() throws Exception
   {
      BundleContext sysContext = getFramework().getBundleContext();

      JavaArchive archiveA = getBundleArchive();
      Bundle bundleA = installBundle(archiveA);
      try
      {
         assertBundleState(Bundle.INSTALLED, bundleA.getState());
         ServiceReference sref = sysContext.getServiceReference(SomeService.class.getName());
         assertNull("ServiceReference null", sref);

         bundleA.start();
         assertBundleState(Bundle.ACTIVE, bundleA.getState());
         sref = sysContext.getServiceReference(SomeService.class.getName());
         assertNotNull("ServiceReference not null", sref);

         Long serviceId = (Long)sref.getProperty(Constants.SERVICE_ID);
         assertNotNull("service.id not null", serviceId);
         Long secondId = (Long)sysContext.getServiceReference(SomeService.class.getName()).getProperty(Constants.SERVICE_ID);
         assertEquals(serviceId, secondId);

         String[] objectClass = (String[])sref.getProperty(Constants.OBJECTCLASS);
         assertNotNull("objectClass not null", objectClass);
         assertEquals("objectClass length", 1, objectClass.length);
         assertEquals(SomeService.class.getName(), objectClass[0]);

         Bundle bundle = sref.getBundle();
         assertNotNull("Bundle not null", bundle);
      }
      finally
      {
         bundleA.uninstall();
      }

      // Verify not getting the ServiceReference after undeploy
      ServiceReference sref = sysContext.getServiceReference(SomeService.class.getName());
      assertNull("ServiceReference null", sref);
   }

   @Test
   public void testServiceReferenceBoth() throws Exception
   {
      BundleContext sysContext = getFramework().getBundleContext();

      JavaArchive archiveA = getBundleArchive();
      Bundle bundleA = installBundle(archiveA);
      try
      {
         bundleA.start();

         Deployment beanB = createDeployment("beanB", null, SomeServiceB.class);
         deploy(addBeanMetaData(beanB, SomeServiceB.class));
         try
         {
            ServiceReference[] srefs = sysContext.getServiceReferences(SomeService.class.getName(), null);
            assertNotNull("ServiceReferences not null", srefs);
            assertEquals("ServiceReferences length", 2, srefs.length);

            ServiceReference srefB = srefs[0];
            ServiceReference srefA = srefs[1];
            assertEquals(bundleA, srefA.getBundle());

            Integer rankingA = (Integer)srefA.getProperty(Constants.SERVICE_RANKING);
            assertNull("service.ranking null", rankingA);

            Integer rankingB = (Integer)srefB.getProperty(Constants.SERVICE_RANKING);
            assertNotNull("service.ranking not null", rankingB);
            assertEquals(Integer.MIN_VALUE, rankingB.intValue());

            ServiceReference sref = sysContext.getServiceReference(SomeService.class.getName());
            assertNotNull("ServiceReference not null", sref);
            assertEquals(srefA, sref);
         }
         finally
         {
            undeploy(beanB);
         }
      }
      finally
      {
         bundleA.uninstall();
      }

      // Verify not getting the ServiceReference after undeploy
      ServiceReference sref = sysContext.getServiceReference(SomeService.class.getName());
      assertNull("ServiceReference null", sref);
   }

   @Test
   public void testServiceReferenceReverse() throws Exception
   {
      BundleContext sysContext = getFramework().getBundleContext();

      Deployment beanB = createDeployment("beanB", null, SomeServiceB.class);
      deploy(addBeanMetaData(beanB, SomeServiceB.class));
      try
      {
         JavaArchive archiveA = getBundleArchive();
         Bundle bundleA = installBundle(archiveA);
         try
         {
            bundleA.start();

            ServiceReference[] srefs = sysContext.getServiceReferences(SomeService.class.getName(), null);
            assertNotNull("ServiceReferences not null", srefs);
            assertEquals("ServiceReferences length", 2, srefs.length);

            ServiceReference srefB = srefs[0];
            ServiceReference srefA = srefs[1];
            assertEquals(bundleA, srefA.getBundle());

            Integer rankingA = (Integer)srefA.getProperty(Constants.SERVICE_RANKING);
            assertNull("service.ranking null", rankingA);

            Integer rankingB = (Integer)srefB.getProperty(Constants.SERVICE_RANKING);
            assertNotNull("service.ranking not null", rankingB);
            assertEquals(Integer.MIN_VALUE, rankingB.intValue());

            ServiceReference sref = sysContext.getServiceReference(SomeService.class.getName());
            assertNotNull("ServiceReference not null", sref);
            assertEquals(srefA, sref);
         }
         finally
         {
            bundleA.uninstall();
         }
      }
      finally
      {
         undeploy(beanB);
      }

      // Verify not getting the ServiceReference after undeploy
      ServiceReference sref = sysContext.getServiceReference(SomeService.class.getName());
      assertNull("ServiceReference null", sref);
   }

   private JavaArchive getBundleArchive()
   {
      // Bundle-SymbolicName: jbosgi326-bundleA
      // Bundle-Activator: org.jboss.test.osgi.core.jbosgi326.OSGi326Activator
      final JavaArchive archiveA = Archives.create("jbosgi326-bundleA", JavaArchive.class);
      archiveA.addClasses(OSGi326Activator.class, SomeService.class, SomeServiceA.class);
      archiveA.setManifest(new Asset()
      {
         public InputStream openStream()
         {
            OSGiManifestBuilder builder = OSGiManifestBuilder.newInstance();
            builder.addBundleManifestVersion(2);
            builder.addBundleSymbolicName(archiveA.getName());
            builder.addBundleActivator(OSGi326Activator.class.getName());
            return builder.openStream();
         }
      });
      return archiveA;
   }
}
