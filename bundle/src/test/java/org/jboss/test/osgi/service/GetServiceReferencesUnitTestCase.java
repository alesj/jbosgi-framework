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
package org.jboss.test.osgi.service;

import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.Test;

import org.jboss.test.osgi.FrameworkTest;
import org.jboss.test.osgi.service.support.a.A;
import org.jboss.test.osgi.service.support.b.B;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * GetServiceReferencesUnitTestCase.
 *
 * todo test service permissions
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @version $Revision: 1.1 $
 */
public class GetServiceReferencesUnitTestCase extends FrameworkTest
{
   public static Test suite()
   {
      return suite(GetServiceReferencesUnitTestCase.class);
   }

   public GetServiceReferencesUnitTestCase(String name)
   {
      super(name);
   }

   public void testGetServiceReferences() throws Exception
   {
      Bundle bundle1 = installBundle(assembleBundle("simple1", "/bundles/simple/simple-bundle1", A.class));
      try
      {
         bundle1.start();
         BundleContext bundleContext1 = bundle1.getBundleContext();
         assertNotNull(bundleContext1);
         
         assertNoGetReference(bundleContext1, A.class.getName());
         assertNoReferences(bundleContext1, A.class.getName());
         assertNoAllReferences(bundleContext1, A.class.getName());
         assertNoGetReference(bundleContext1, B.class.getName());
         assertNoReferences(bundleContext1, B.class.getName());
         assertNoAllReferences(bundleContext1, B.class.getName());

         Class<?> clazz = bundle1.loadClass(A.class.getName());
         Object service1 = clazz.newInstance();
         ServiceRegistration registration1 = bundleContext1.registerService(A.class.getName(), service1, null);
         assertNotNull(registration1);
         ServiceReference reference1 = registration1.getReference();
         assertNotNull(reference1);

         assertGetReference(bundleContext1, A.class.getName(), reference1);
         assertReferences(bundleContext1, A.class.getName(), reference1);
         assertAllReferences(bundleContext1, A.class.getName(), reference1);
         assertNoGetReference(bundleContext1, B.class.getName());
         assertNoReferences(bundleContext1, B.class.getName());
         assertNoAllReferences(bundleContext1, B.class.getName());
         
         registration1.unregister();
         
         assertNoGetReference(bundleContext1, A.class.getName());
         assertNoReferences(bundleContext1, A.class.getName());
         assertNoAllReferences(bundleContext1, A.class.getName());
         assertNoGetReference(bundleContext1, B.class.getName());
         assertNoReferences(bundleContext1, B.class.getName());
         assertNoAllReferences(bundleContext1, B.class.getName());
         
         try
         {
            bundleContext1.getServiceReference(null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalArgumentException.class, t);
         }
         
         try
         {
            bundleContext1.getServiceReferences(null, "invalid");
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(InvalidSyntaxException.class, t);
         }
         
         try
         {
            bundleContext1.getAllServiceReferences(null, "invalid");
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(InvalidSyntaxException.class, t);
         }
         
         bundle1.stop();
         
         try
         {
            bundleContext1.getServiceReference(A.class.getName());
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalStateException.class, t);
         }
         
         try
         {
            bundleContext1.getServiceReferences(null, null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalStateException.class, t);
         }
         
         try
         {
            bundleContext1.getAllServiceReferences(null, null);
            fail("Should not be here!");
         }
         catch (Throwable t)
         {
            checkThrowable(IllegalStateException.class, t);
         }
      }
      finally
      {
         uninstall(bundle1);
      }
   }
   
   public void testGetServiceReferencesNoClassNotAssignable() throws Exception
   {
      assertGetServiceReferencesNotAssignable(null);
   }
   
   public void testGetServiceReferencesNotAssignable() throws Exception
   {
      assertGetServiceReferencesNotAssignable(A.class.getName());
   }
   
   public void assertGetServiceReferencesNotAssignable(String className) throws Exception
   {
      Bundle bundle1 = installBundle(assembleBundle("simple1", "/bundles/simple/simple-bundle1", A.class));
      try
      {
         bundle1.start();
         BundleContext bundleContext1 = bundle1.getBundleContext();
         assertNotNull(bundleContext1);
         
         if (className != null)
            assertNoGetReference(bundleContext1, className);

         Class<?> clazz = bundle1.loadClass(A.class.getName());
         Object service1 = clazz.newInstance();
         ServiceRegistration registration1 = bundleContext1.registerService(A.class.getName(), service1, null);
         assertNotNull(registration1);
         ServiceReference reference1 = registration1.getReference();
         assertNotNull(reference1);

         Bundle bundle2 = installBundle(assembleBundle("simple2", "/bundles/simple/simple-bundle2", A.class));
         try
         {
            bundle2.start();
            BundleContext bundleContext2 = bundle2.getBundleContext();
            assertNotNull(bundleContext2);

            if (className != null)
               assertNoGetReference(bundleContext2, className);

            clazz = bundle2.loadClass(A.class.getName());
            Object service2 = clazz.newInstance();
            ServiceRegistration registration2 = bundleContext2.registerService(A.class.getName(), service2, null);
            assertNotNull(registration2);
            ServiceReference reference2 = registration2.getReference();
            assertNotNull(reference2);
            
            if (className != null)
               assertGetReference(bundleContext1, className, reference1);

            if (className != null)
               assertGetReference(bundleContext2, className, reference2);
            
            registration1.unregister();
            
            if (className != null)
               assertNoGetReference(bundleContext1, className);

            if (className != null)
               assertGetReference(bundleContext2, className, reference2);

            registration1 = bundleContext1.registerService(A.class.getName(), service1, null);
            assertNotNull(registration1);
            reference1 = registration1.getReference();
            assertNotNull(reference1);
            
            if (className != null)
               assertGetReference(bundleContext1, className, reference1);

            if (className != null)
               assertGetReference(bundleContext2, className, reference2);
            
            registration2.unregister();
            
            if (className != null)
               assertGetReference(bundleContext1, className, reference1);

            if (className != null)
               assertNoGetReference(bundleContext2, className);
            
            registration1.unregister();
            
            if (className != null)
               assertNoGetReference(bundleContext1, className);

            if (className != null)
               assertNoGetReference(bundleContext2, className);
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

   public void testGetServiceReferencesNoClassAssignable() throws Exception
   {
      assertGetServiceReferencesAssignable(null);
   }

   public void testGetServiceReferencesClassAssignable() throws Exception
   {
      assertGetServiceReferencesAssignable(A.class.getName());
   }

   public void assertGetServiceReferencesAssignable(String className) throws Exception
   {
      Bundle bundle1 = installBundle(assembleBundle("service2", "/bundles/service/service-bundle2", A.class));
      try
      {
         bundle1.start();
         BundleContext bundleContext1 = bundle1.getBundleContext();
         assertNotNull(bundleContext1);

         if (className != null)
            assertNoGetReference(bundleContext1, className);

         Class<?> clazz = bundle1.loadClass(A.class.getName());
         Object service1 = clazz.newInstance();
         ServiceRegistration registration1 = bundleContext1.registerService(A.class.getName(), service1, null);
         assertNotNull(registration1);
         ServiceReference reference1 = registration1.getReference();
         assertNotNull(reference1);

         Bundle bundle2 = installBundle(assembleBundle("service1", "/bundles/service/service-bundle1"));
         try
         {
            bundle2.start();
            BundleContext bundleContext2 = bundle2.getBundleContext();
            assertNotNull(bundleContext2);

            if (className != null)
               assertGetReference(bundleContext2, className, reference1);

            clazz = bundle2.loadClass(A.class.getName());
            Object service2 = clazz.newInstance();
            ServiceRegistration registration2 = bundleContext2.registerService(A.class.getName(), service2, null);
            assertNotNull(registration2);
            ServiceReference reference2 = registration2.getReference();
            assertNotNull(reference2);
            
            if (className != null)
               assertGetReference(bundleContext1, className, reference1);

            if (className != null)
               assertGetReference(bundleContext2, className, reference1);
            
            registration1.unregister();
            
            if (className != null)
               assertGetReference(bundleContext1, className, reference2);

            if (className != null)
               assertGetReference(bundleContext2, className, reference2);

            registration1 = bundleContext1.registerService(A.class.getName(), service1, null);
            assertNotNull(registration1);
            reference1 = registration1.getReference();
            assertNotNull(reference1);
            
            if (className != null)
               assertGetReference(bundleContext1, className, reference2);

            if (className != null)
               assertGetReference(bundleContext2, className, reference2);
            
            registration2.unregister();
            
            if (className != null)
               assertGetReference(bundleContext1, className, reference1);

            if (className != null)
               assertGetReference(bundleContext2, className, reference1);
            
            registration1.unregister();
            
            if (className != null)
               assertNoGetReference(bundleContext1, className);

            if (className != null)
               assertNoGetReference(bundleContext2, className);
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

   public void testGetServiceReferencesRankings() throws Exception
   {
      String className = A.class.getName();
      
      Bundle bundle1 = installBundle(assembleBundle("service2", "/bundles/service/service-bundle2", A.class));
      try
      {
         bundle1.start();
         BundleContext bundleContext1 = bundle1.getBundleContext();
         assertNotNull(bundleContext1);
         
         assertNoGetReference(bundleContext1, className);
         assertNoReferences(bundleContext1, className);
         assertNoAllReferences(bundleContext1, className);

         Dictionary<String, Object> properties1 = new Hashtable<String, Object>();
         properties1.put(Constants.SERVICE_RANKING, 1);
         Class<?> clazz = bundle1.loadClass(className);
         Object service1 = clazz.newInstance();
         ServiceRegistration registration1 = bundleContext1.registerService(className, service1, properties1);
         assertNotNull(registration1);
         ServiceReference reference1 = registration1.getReference();
         assertNotNull(reference1);

         Bundle bundle2 = installBundle(assembleBundle("service1", "/bundles/service/service-bundle1"));
         try
         {
            bundle2.start();
            BundleContext bundleContext2 = bundle2.getBundleContext();
            assertNotNull(bundleContext2);

            assertGetReference(bundleContext2, className, reference1);
            assertReferences(bundleContext2, className,  reference1);
            assertAllReferences(bundleContext2, className,  reference1);

            Dictionary<String, Object> properties2 = new Hashtable<String, Object>();
            properties2.put(Constants.SERVICE_RANKING, 2);
            clazz = bundle2.loadClass(className);
            Object service2 = clazz.newInstance();
            ServiceRegistration registration2 = bundleContext2.registerService(className, service2, properties2);
            assertNotNull(registration2);
            ServiceReference reference2 = registration2.getReference();
            assertNotNull(reference2);
            
            assertGetReference(bundleContext1, className, reference2);
            assertReferences(bundleContext1, className,  reference2, reference1);
            assertAllReferences(bundleContext1, className,  reference2, reference1);

            assertGetReference(bundleContext2, className, reference2);
            assertReferences(bundleContext2, className,  reference2, reference1);
            assertAllReferences(bundleContext2, className,  reference2, reference1);
            
            registration1.unregister();
            
            assertGetReference(bundleContext1, className, reference2);
            assertReferences(bundleContext1, className,  reference2);
            assertAllReferences(bundleContext1, className,  reference2);

            assertGetReference(bundleContext2, className, reference2);
            assertReferences(bundleContext2, className,  reference2);
            assertAllReferences(bundleContext2, className,  reference2);

            registration1 = bundleContext1.registerService(className, service1, properties1);
            assertNotNull(registration1);
            reference1 = registration1.getReference();
            assertNotNull(reference1);
            
            assertGetReference(bundleContext1, className, reference2);
            assertReferences(bundleContext1, className,  reference2, reference1);
            assertAllReferences(bundleContext1, className,  reference2, reference1);

            assertGetReference(bundleContext2, className, reference2);
            assertReferences(bundleContext2, className,  reference2, reference1);
            assertAllReferences(bundleContext2, className,  reference2, reference1);
            
            registration2.unregister();
            
            assertGetReference(bundleContext1, className, reference1);
            assertReferences(bundleContext1, className,  reference1);
            assertAllReferences(bundleContext1, className,  reference1);

            assertGetReference(bundleContext2, className, reference1);
            assertReferences(bundleContext2, className,  reference1);
            assertAllReferences(bundleContext2, className,  reference1);
            
            registration1.unregister();
            
            assertNoGetReference(bundleContext1, className);
            assertNoReferences(bundleContext1, className);
            assertNoAllReferences(bundleContext1, className);

            if (className != null)
               assertNoGetReference(bundleContext2, className);
            assertNoReferences(bundleContext2, className);
            assertNoAllReferences(bundleContext2, className);
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
   
   public void testGetServiceReferencesFilterted() throws Exception
   {
      String className = A.class.getName();
      String wrongClassName = B.class.getName();
      
      Bundle bundle1 = installBundle(assembleBundle("simple1", "/bundles/simple/simple-bundle1", A.class));
      try
      {
         bundle1.start();
         BundleContext bundleContext1 = bundle1.getBundleContext();
         assertNotNull(bundleContext1);
         
         assertNoGetReference(bundleContext1, A.class.getName());
         assertNoReferences(bundleContext1, null, "(a=b)");
         assertNoAllReferences(bundleContext1, null, "(a=b)");
         assertNoReferences(bundleContext1, className, "(a=b)");
         assertNoAllReferences(bundleContext1, className, "(a=b)");
         assertNoReferences(bundleContext1, wrongClassName, "(a=b)");
         assertNoAllReferences(bundleContext1, wrongClassName, "(a=b)");
         assertNoReferences(bundleContext1, null, "(c=d)");
         assertNoAllReferences(bundleContext1, null, "(c=d)");
         assertNoReferences(bundleContext1, className, "(c=d)");
         assertNoAllReferences(bundleContext1, className, "(c=d)");
         assertNoReferences(bundleContext1, wrongClassName, "(c=d)");
         assertNoAllReferences(bundleContext1, wrongClassName, "(c=d)");
         assertNoReferences(bundleContext1, null, "(c=x)");
         assertNoAllReferences(bundleContext1, null, "(c=x)");
         assertNoReferences(bundleContext1, className, "(c=x)");
         assertNoAllReferences(bundleContext1, className, "(c=x)");
         assertNoReferences(bundleContext1, wrongClassName, "(c=x)");
         assertNoAllReferences(bundleContext1, wrongClassName, "(c=x)");
         assertNoReferences(bundleContext1, null, "(x=d)");
         assertNoAllReferences(bundleContext1, null, "(x=d)");
         assertNoReferences(bundleContext1, className, "(x=d)");
         assertNoAllReferences(bundleContext1, className, "(x=d)");
         assertNoReferences(bundleContext1, wrongClassName, "(x=d)");
         assertNoAllReferences(bundleContext1, wrongClassName, "(x=d)");

         Dictionary<String, Object> properties = new Hashtable<String, Object>();
         properties.put("a", "b");
         properties.put("c", "d");
         
         Class<?> clazz = bundle1.loadClass(A.class.getName());
         Object service1 = clazz.newInstance();
         ServiceRegistration registration1 = bundleContext1.registerService(A.class.getName(), service1, properties);
         assertNotNull(registration1);
         ServiceReference reference1 = registration1.getReference();
         assertNotNull(reference1);

         assertGetReference(bundleContext1, A.class.getName(), reference1);
         assertReferences(bundleContext1, null, "(a=b)", reference1);
         assertAllReferences(bundleContext1, null, "(a=b)", reference1);
         assertReferences(bundleContext1, className, "(a=b)", reference1);
         assertAllReferences(bundleContext1, className, "(a=b)", reference1);
         assertNoReferences(bundleContext1, wrongClassName, "(a=b)");
         assertNoAllReferences(bundleContext1, wrongClassName, "(a=b)");
         assertReferences(bundleContext1, null, "(c=d)", reference1);
         assertAllReferences(bundleContext1, null, "(c=d)", reference1);
         assertReferences(bundleContext1, className, "(c=d)", reference1);
         assertAllReferences(bundleContext1, className, "(c=d)", reference1);
         assertNoReferences(bundleContext1, wrongClassName, "(c=d)");
         assertNoAllReferences(bundleContext1, wrongClassName, "(c=d)");
         assertNoReferences(bundleContext1, null, "(c=x)");
         assertNoAllReferences(bundleContext1, null, "(c=x)");
         assertNoReferences(bundleContext1, className, "(c=x)");
         assertNoAllReferences(bundleContext1, className, "(c=x)");
         assertNoReferences(bundleContext1, wrongClassName, "(c=x)");
         assertNoAllReferences(bundleContext1, wrongClassName, "(c=x)");
         assertNoReferences(bundleContext1, null, "(x=d)");
         assertNoAllReferences(bundleContext1, null, "(x=d)");
         assertNoReferences(bundleContext1, className, "(x=d)");
         assertNoAllReferences(bundleContext1, className, "(x=d)");
         assertNoReferences(bundleContext1, wrongClassName, "(x=d)");
         assertNoAllReferences(bundleContext1, wrongClassName, "(x=d)");
         
         registration1.unregister();
         
         assertNoGetReference(bundleContext1, A.class.getName());
         assertNoReferences(bundleContext1, null, "(a=b)");
         assertNoAllReferences(bundleContext1, null, "(a=b)");
         assertNoReferences(bundleContext1, className, "(a=b)");
         assertNoAllReferences(bundleContext1, className, "(a=b)");
         assertNoReferences(bundleContext1, wrongClassName, "(a=b)");
         assertNoAllReferences(bundleContext1, wrongClassName, "(a=b)");
         assertNoReferences(bundleContext1, null, "(c=d)");
         assertNoAllReferences(bundleContext1, null, "(c=d)");
         assertNoReferences(bundleContext1, className, "(c=d)");
         assertNoAllReferences(bundleContext1, className, "(c=d)");
         assertNoReferences(bundleContext1, wrongClassName, "(c=d)");
         assertNoAllReferences(bundleContext1, wrongClassName, "(c=d)");
         assertNoReferences(bundleContext1, null, "(c=x)");
         assertNoAllReferences(bundleContext1, null, "(c=x)");
         assertNoReferences(bundleContext1, className, "(c=x)");
         assertNoAllReferences(bundleContext1, className, "(c=x)");
         assertNoReferences(bundleContext1, wrongClassName, "(c=x)");
         assertNoAllReferences(bundleContext1, wrongClassName, "(c=x)");
         assertNoReferences(bundleContext1, null, "(x=d)");
         assertNoAllReferences(bundleContext1, null, "(x=d)");
         assertNoReferences(bundleContext1, className, "(x=d)");
         assertNoAllReferences(bundleContext1, className, "(x=d)");
         assertNoReferences(bundleContext1, wrongClassName, "(x=d)");
         assertNoAllReferences(bundleContext1, wrongClassName, "(x=d)");
      }
      finally
      {
         uninstall(bundle1);
      }
   }
}
