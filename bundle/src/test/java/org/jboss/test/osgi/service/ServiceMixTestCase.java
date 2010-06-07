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
package org.jboss.test.osgi.service;

// $Id: $

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.osgi.framework.deployers.AbstractDeployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.test.osgi.service.support.LazyBundle;
import org.jboss.test.osgi.service.support.a.A;
import org.jboss.test.osgi.service.support.a.AMBean;
import org.jboss.test.osgi.service.support.c.C;
import org.jboss.test.osgi.service.support.d.ServiceMixFactory;
import org.jboss.test.osgi.service.support.e.E;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Test MC's service mixture.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author thomas.diesler@jboss.com
 */
public class ServiceMixTestCase extends AbstractServiceMixTest
{
   @Test
   public void testGetServiceReferenceFromMC() throws Throwable
   {
      Deployment beans = createDeployment("beanA", null, A.class);
      deploy(addBeanMetaData(beans, A.class));
      try
      {
         // Bundle-SymbolicName: org.jboss.test.osgi.service1
         // Import-Package: org.jboss.test.osgi.service.support.a
         Archive<?> assembly2 = assembleArchive("simple1", "/bundles/service/service-bundle1");
         Bundle bundle2 = installBundle(assembly2);
         try
         {
            bundle2.start();
            BundleContext bundleContext1 = bundle2.getBundleContext();
            assertNotNull(bundleContext1);

            ServiceReference ref1 = bundleContext1.getServiceReference(A.class.getName());
            assertNotNull(ref1);
            try
            {
               Bundle refsBundle = ref1.getBundle();
               assertEquals(refsBundle, getBundle(beans));

               assertNotNull(bundleContext1.getService(ref1));
               assertUsingBundles(ref1, bundle2);

               ServiceReference[] inUse = bundle2.getServicesInUse();
               assertArrayEquals(new ServiceReference[] { ref1 }, inUse);
            }
            finally
            {
               bundleContext1.ungetService(ref1);
            }

            KernelControllerContext beanKCC = getControllerContext("A");
            change(beanKCC, ControllerState.DESCRIBED);

            assertNull(bundleContext1.getServiceReference(A.class.getName()));
         }
         finally
         {
            bundle2.uninstall();
         }
      }
      finally
      {
         undeploy(beans);
      }
   }

   @Test
   public void testInjectionToMC() throws Throwable
   {
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder("C", C.class.getName());
      builder.addPropertyMetaData("a", builder.createContextualInject());
      
      Deployment beans = createDeployment("beanA", null, A.class, C.class);
      addBeanMetaData(beans, builder.getBeanMetaData());
      addDeployment(beans);
      try
      {
         KernelControllerContext kcc = getControllerContext("C", null);

         // Bundle-SymbolicName: org.jboss.test.osgi.service3
         // Import-Package: org.jboss.test.osgi.service.support.a,org.jboss.test.osgi.service.support.c
         Archive<?> assembly1 = assembleArchive("simple2", "/bundles/service/service-bundle3");
         Bundle bundle1 = installBundle(assembly1);
         try
         {
            bundle1.start();
            BundleContext bundleContext1 = bundle1.getBundleContext();
            assertNotNull(bundleContext1);

            Class<?> aClass = bundle1.loadClass(A.class.getName());
            Object a = aClass.newInstance();
            ServiceRegistration reg1 = bundleContext1.registerService(A.class.getName(), a, null);
            assertNotNull(reg1);
            try
            {
               checkComplete();

               Object c = getBean("C");
               assertSame(a, getter(c, "getA", "C"));

               ServiceReference ref1 = bundleContext1.getServiceReference(A.class.getName());
               assertUsingBundles(ref1, LazyBundle.getBundle(getDeploymentUnit(beans)));

               change(kcc, ControllerState.DESCRIBED);
               // we did un-injection, should be removed now
               assertUsingBundles(ref1);

               change(kcc, ControllerState.INSTALLED);
               assertEquals(ControllerState.INSTALLED, kcc.getState());
            }
            finally
            {
               reg1.unregister();
            }

            // check if the bean was unwinded as well
            assertEquals(ControllerState.INSTANTIATED, kcc.getState());
         }
         finally
         {
            bundle1.uninstall();
         }
      }
      finally
      {
         undeploy(beans);
      }
   }

   @Test
   public void testInjectionToMCNamedService() throws Throwable
   {
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder("C", C.class.getName());
      builder.addPropertyMetaData("a", builder.createInject("A"));

      Deployment beans = createDeployment("beanA", null, A.class, C.class);
      addBeanMetaData(beans, builder.getBeanMetaData());
      addDeployment(beans);
      try
      {
         KernelControllerContext kcc = getControllerContext("C", null);

         // Bundle-SymbolicName: org.jboss.test.osgi.service3
         // Import-Package: org.jboss.test.osgi.service.support.a,org.jboss.test.osgi.service.support.c
         Archive<?> assembly1 = assembleArchive("simple2", "/bundles/service/service-bundle3");
         Bundle bundle1 = installBundle(assembly1);
         try
         {
            bundle1.start();
            BundleContext bundleContext1 = bundle1.getBundleContext();
            assertNotNull(bundleContext1);

            Class<?> aClass = bundle1.loadClass(A.class.getName());
            Object a = aClass.newInstance();
            Hashtable<String, Object> table = new Hashtable<String, Object>();
            table.put("service.alias.1", "A");
            ServiceRegistration reg1 = bundleContext1.registerService(A.class.getName(), a, table);
            assertNotNull(reg1);
            try
            {
               checkComplete();

               Object c = getBean("C");
               assertSame(a, getter(c, "getA", "C"));

               ServiceReference ref1 = bundleContext1.getServiceReference(A.class.getName());
               assertUsingBundles(ref1, LazyBundle.getBundle(getDeploymentUnit(beans)));

               change(kcc, ControllerState.DESCRIBED);
               // we did un-injection, should be removed now
               assertUsingBundles(ref1);

               change(kcc, ControllerState.INSTALLED);
               assertEquals(ControllerState.INSTALLED, kcc.getState());
            }
            finally
            {
               reg1.unregister();
            }

            // check if the bean was unwinded as well
            assertEquals(ControllerState.INSTANTIATED, kcc.getState());
         }
         finally
         {
            bundle1.uninstall();
         }
      }
      finally
      {
         undeploy(beans);
      }
   }

   @Test
   public void testInvokeDispatch() throws Throwable
   {
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder("C", C.class.getName());
      builder.addPropertyMetaData("msg", builder.createInject("A", "msg"));
      builder.addInstall("calc", "A", int.class.getName(), 123);
      Deployment beans = createDeployment("beanA", null, A.class, C.class);
      addBeanMetaData(beans, builder.getBeanMetaData());
      addDeployment(beans);
      try
      {
         // Bundle-SymbolicName: org.jboss.test.osgi.service3
         // Import-Package: org.jboss.test.osgi.service.support.a,org.jboss.test.osgi.service.support.c
         Archive<?> assembly1 = assembleArchive("simple2", "/bundles/service/service-bundle3");
         Bundle bundle1 = installBundle(assembly1);
         try
         {
            bundle1.start();
            BundleContext bundleContext1 = bundle1.getBundleContext();
            assertNotNull(bundleContext1);

            Class<?> aClass = bundle1.loadClass(A.class.getName());
            Object a = aClass.newInstance();
            setter(a, "setMsg", "HelloWorld!", "A");
            Hashtable<String, Object> table = new Hashtable<String, Object>();
            table.put("service.alias.1", "A");
            ServiceRegistration reg1 = bundleContext1.registerService(A.class.getName(), a, table);
            assertNotNull(reg1);
            try
            {
               checkComplete();

               Object c = getBean("C");
               assertSame(getter(a, "getMsg", "A"), getter(c, "getMsg", "C"));
               assertEquals(123, getter(a, "getX", "A"));
            }
            finally
            {
               reg1.unregister();
            }
         }
         finally
         {
            bundle1.uninstall();
         }
      }
      finally
      {
         undeploy(beans);
      }
   }

   @Test
   @SuppressWarnings("rawtypes")
   public void testServiceFactoryInjection() throws Throwable
   {
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder("C1", C.class.getName());
      builder.addPropertyMetaData("a", builder.createInject("A"));
      Deployment beans1 = createDeployment("beanA1", null, A.class, C.class, ServiceMixFactory.class);
      addBeanMetaData(beans1, builder.getBeanMetaData());
      addDeployment(beans1);
      try
      {
         builder = BeanMetaDataBuilder.createBuilder("C2", C.class.getName());
         builder.addPropertyMetaData("a", builder.createInject("A"));
         Deployment beans2 = createDeployment("beanA2", null, E.class);
         addBeanMetaData(beans2, builder.getBeanMetaData());
         addDeployment(beans2);
         try
         {
            // Bundle-SymbolicName: org.jboss.test.osgi.service4
            // Import-Package: org.jboss.test.osgi.service.support.a,org.jboss.test.osgi.service.support.c,org.jboss.test.osgi.service.support.d,org.osgi.framework
            Archive<?> assembly1 = assembleArchive("simple2", "/bundles/service/service-bundle4");
            Bundle bundle1 = installBundle(assembly1);
            try
            {
               bundle1.start();
               BundleContext bundleContext1 = bundle1.getBundleContext();
               assertNotNull(bundleContext1);

               Class<?> dClass = bundle1.loadClass(ServiceMixFactory.class.getName());
               Object d = dClass.newInstance();
               Hashtable<String, Object> table = new Hashtable<String, Object>();
               table.put("service.alias.1", "A");
               ServiceRegistration reg1 = bundleContext1.registerService(A.class.getName(), d, table);
               assertNotNull(reg1);

               Object a1 = null;
               Object a2 = null;
               try
               {
                  checkComplete();

                  Object c1 = getBean("C1");
                  a1 = getter(c1, "getA", "C1");
                  Object msg1 = getter(a1, "getMsg", "A1");
                  assertEquals(msg1, getBundle(beans1).getSymbolicName());

                  Object c2 = getBean("C2");
                  a2 = getter(c2, "getA", "C2");
                  Object msg2 = getter(a2, "getMsg", "A2");
                  assertEquals(msg2, getBundle(beans2).getSymbolicName());
               }
               finally
               {
                  reg1.unregister();
               }

               List as = assertInstanceOf(getter(d, "getAs", "A"), List.class);
               assertNotNull(as);
               assertEquals(2, as.size());
               assertTrue(as.contains(a1));
               assertTrue(as.contains(a2));
            }
            finally
            {
               bundle1.uninstall();
            }
         }
         finally
         {
            undeploy(beans2);
         }
      }
      finally
      {
         undeploy(beans1);
      }
   }

   @Test
   @SuppressWarnings("rawtypes")
   public void testServiceFactoryMix() throws Throwable
   {
      BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder("C1", C.class.getName());
      builder.addPropertyMetaData("a", builder.createInject("A"));
      Deployment beans1 = createDeployment("beanA1", null, A.class, C.class, ServiceMixFactory.class);
      addBeanMetaData(beans1, builder.getBeanMetaData());
      addDeployment(beans1);
      try
      {
         // Bundle-SymbolicName: org.jboss.test.osgi.service4
         // Import-Package: org.jboss.test.osgi.service.support.a,org.jboss.test.osgi.service.support.c,org.jboss.test.osgi.service.support.d,org.osgi.framework
         Archive<?> assembly = assembleArchive("service-bundle4", "/bundles/service/service-bundle4");
         Bundle bundle = installBundle(assembly);
         try
         {
            bundle.start();
            BundleContext bundleContext = bundle.getBundleContext();
            assertNotNull(bundleContext);

            // Construct the ServiceFactory
            Class<?> factoryClass = bundle.loadClass(ServiceMixFactory.class.getName());
            Object factory = factoryClass.newInstance();

            // Register the ServiceFactory
            Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put("service.alias.1", "A");
            ServiceRegistration sreg = bundleContext.registerService(A.class.getName(), factory, props);
            assertNotNull(sreg);
            ServiceReference sref = sreg.getReference();
            assertNotNull(sref);

            Object a = null;
            try
            {
               checkComplete();

               Object c1 = getBean("C1");
               a = getter(c1, "getA", "C1");

               ServiceReference refC = bundleContext.getServiceReference(C.class.getName());
               Bundle beanBundle = refC.getBundle();
               assertNotNull(beanBundle);
               BundleContext beanBundleContext = beanBundle.getBundleContext();
               assertNotNull(beanBundleContext);
               Object service = beanBundleContext.getService(sref);
               assertSame(a, service);

               KernelControllerContext cCC = getControllerContext("C1", null);
               change(cCC, ControllerState.INSTANTIATED);

               List as = assertInstanceOf(getter(factory, "getAs", "A"), List.class);
               assertNotNull(as);

               System.out.println("FIXME: Verify ServiceFactory still in use");
               //assertTrue(as.isEmpty()); // SF is still in use
            }
            finally
            {
               sreg.unregister();
            }

            List as = assertInstanceOf(getter(factory, "getAs", "A"), List.class);
            assertNotNull(as);

            System.out.println("FIXME: Verify ServiceFactory still in use");
            //assertEquals(1, as.size());
            //assertTrue(as.contains(a));
         }
         finally
         {
            bundle.uninstall();
         }
      }
      finally
      {
         undeploy(beans1);
      }
   }

   @Test
   public void testFiltering() throws Throwable
   {
      Deployment beans1 = createDeployment("beanA", null, A.class);
      deploy(addBeanMetaData(beans1, A.class));
      try
      {
         // Bundle-SymbolicName: org.jboss.test.osgi.service1
         // Import-Package: org.jboss.test.osgi.service.support.a
         Archive<?> assembly1 = assembleArchive("simple2", "/bundles/service/service-bundle1");
         Bundle bundle1 = installBundle(assembly1);
         try
         {
            bundle1.start();
            BundleContext bundleContext1 = bundle1.getBundleContext();
            assertNotNull(bundleContext1);

            Class<?> aClass = bundle1.loadClass(A.class.getName());
            Object a = aClass.newInstance();
            Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
            dictionary.put("a", "b");
            ServiceRegistration reg1 = bundleContext1.registerService(A.class.getName(), a, dictionary);
            assertNotNull(reg1);
            try
            {
               ServiceReference[] refs = bundleContext1.getServiceReferences(A.class.getName(), "(a=b)");
               assertNotNull(refs);
               assertEquals(1, refs.length);
               ServiceReference osgiRef = refs[0];

               refs = bundleContext1.getServiceReferences(A.class.getName(), "(bean.name=A)");
               assertNotNull(refs);
               assertEquals(1, refs.length);
               ServiceReference mcRef = refs[0];

               // OSGi service should bubble to top
               assertEquals("OSGi service has not bubbled on top", osgiRef, bundleContext1.getServiceReference(A.class.getName()));
               assertTrue(osgiRef.compareTo(mcRef) > 0);

               // lowest ranking first
               refs = bundleContext1.getServiceReferences(A.class.getName(), null);
               assertNotNull(refs);
               assertEquals(2, refs.length);
               assertEquals(mcRef, refs[0]);
               assertEquals(osgiRef, refs[1]);
            }
            finally
            {
               reg1.unregister();
            }
         }
         finally
         {
            bundle1.uninstall();
         }
      }
      finally
      {
         undeploy(beans1);
      }
   }

   @Test
   public void testBeansMix() throws Throwable
   {
      Archive<?> assembly = assembleArchive("beans1", "/bundles/service/service-beans1", A.class);
      Deployment deployment = deploy(AbstractDeployment.createDeployment(toVirtualFile(assembly)));
      try
      {
         Bundle bundle = getBundle(getDeploymentUnit(deployment));
         bundle.start();

         ServiceReference[] refs = bundle.getRegisteredServices();
         assertNotNull(refs);
         assertEquals(1, refs.length);
         ServiceReference ref = refs[0];
         assertEquals(bundle, ref.getBundle());
         Class<?> aClass = bundle.loadClass(A.class.getName());
         BundleContext bc = bundle.getBundleContext();
         assertNotNull(bc);
         Object service = bc.getService(ref);
         assertInstanceOf(service, aClass, false);
         assertSame(service, getBean("A"));
         assertTrue(bc.ungetService(ref));
         assertFalse(bc.ungetService(ref));
      }
      finally
      {
         undeploy(deployment);
      }
   }

   @Test
   public void testServiceInjection() throws Throwable
   {
      Archive<?> assembly1 = assembleArchive("simple2", "/bundles/service/service-bundle2", A.class);
      Bundle bundle = installBundle(assembly1);
      try
      {
         bundle.start();
         BundleContext bundleContext1 = bundle.getBundleContext();
         assertNotNull(bundleContext1);

         Class<?> aClass = bundle.loadClass(A.class.getName());
         Object a = aClass.newInstance();
         Hashtable<String, Object> table = new Hashtable<String, Object>();
         table.put("a", "b");
         ServiceRegistration reg1 = bundleContext1.registerService(A.class.getName(), a, table);
         assertNotNull(reg1);

         Archive<?> assembly = assembleArchive("beans1", "/bundles/service/service-beans2", C.class);
         Deployment beans = deploy(AbstractDeployment.createDeployment(toVirtualFile(assembly)));
         try
         {
            Bundle beansBundle = getBundle(getDeploymentUnit(beans));
            beansBundle.start();

            Object c = getBean("C");
            assertEquals(a, getter(c, "getA", "C"));
         }
         finally
         {
            undeploy(beans);
         }
      }
      finally
      {
         bundle.uninstall();
      }
   }

   @Test
   public void testExposedClasses() throws Throwable
   {
      Archive<?> assembly = assembleArchive("beans3", "/bundles/service/service-beans3", A.class);
      Deployment beans = deploy(AbstractDeployment.createDeployment(toVirtualFile(assembly)));
      try
      {
         Bundle bundle = getBundle(getDeploymentUnit(beans));
         bundle.start();

         // Bundle-SymbolicName: org.jboss.test.osgi.service1
         // Import-Package: org.jboss.test.osgi.service.support.a
         Archive<?> assembly1 = assembleArchive("simple1", "/bundles/service/service-bundle1");
         Bundle bundle1 = installBundle(assembly1);
         try
         {
            bundle1.start();
            BundleContext bundleContext = bundle1.getBundleContext();
            assertNotNull(bundleContext);         

            ServiceReference[] refs = bundleContext.getServiceReferences(A.class.getName(), null);
            assertNull(refs);
            refs = bundleContext.getServiceReferences(AMBean.class.getName(), null);
            assertNotNull(refs);
         }
         finally
         {
            bundle1.uninstall();
         }
      }
      finally
      {
         undeploy(beans);
      }
   }
}
