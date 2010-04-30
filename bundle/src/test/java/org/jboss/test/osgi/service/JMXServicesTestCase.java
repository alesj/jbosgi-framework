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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;

import javax.management.ObjectName;

import org.jboss.test.osgi.AbstractDeploymentTest;
import org.jboss.test.osgi.service.support.MockInvokerMBean;
import org.junit.Test;

/**
 * Test MC's jmx support.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author thomas.diesler@jboss.com
 */
public class JMXServicesTestCase extends AbstractDeploymentTest
{
   @Test
   public void testAtJmx() throws Throwable
   {
      URL beans = getResourceURL("service/jmx-services.xml");
      deploy(beans);
      try
      {
         MockInvokerMBean invoker = (MockInvokerMBean)getBean("Invoker");
         assertNotNull(invoker.getServer());
         ObjectName name = invoker.getServiceName();
         assertNotNull(name);
         assertEquals("jboss:service=invoker,type=unified", name.getCanonicalName());
      }
      finally
      {
         undeploy(beans);
      }
   }

   @Test
   public void testMBeans() throws Throwable
   {
      System.out.println("FIXME [JBOSGI-141] Service integration with MC");
   }
   
   /*
   public void testAtJmx() throws Throwable
   {
      MockInvokerMBean invoker = (MockInvokerMBean) getBean("Invoker");
      assertNotNull(invoker.getServer());
      ObjectName name = invoker.getServiceName();
      assertNotNull(name);
      assertEquals("jboss:service=invoker,type=unified", name.getCanonicalName());
   }

   public void testMBeans() throws Throwable
   {
      AssembledDirectory mix = createAssembledDirectory("jmx1", "");
      addPath(mix, "/bundles/service/service-jmx1", "");
      addPackage(mix, A.class);
      Deployment deployment = addDeployment(mix);
      try
      {
         checkComplete();

         Bundle bundle = getBundle(getDeploymentUnit(deployment));
         bundle.start();

         Object a = getService("test:service=A");
         assertNotNull(a);         

         ServiceReference[] refs = bundle.getRegisteredServices();
         assertNotNull(refs);
         assertEquals(1, refs.length);
         ServiceReference ref = refs[0];
         assertEquals(bundle, ref.getBundle());
         assertEquals("test:service=A", ref.getProperty("bean.name"));
         Class<?> aClass = bundle.loadClass(A.class.getName());
         BundleContext bc = bundle.getBundleContext();
         assertNotNull(bc);
         Object service = bc.getService(ref);
         assertInstanceOf(service, aClass, false);
         assertSame(service, a);
         assertFalse(bc.ungetService(ref));
      }
      finally
      {
         undeploy(deployment);
      }
   }

   public void testInjectionIntoJMX() throws Throwable
   {
      ServiceMetaData smd = new ServiceMetaData();
      smd.setConstructor(new ServiceConstructorMetaData());
      smd.setCode(C.class.getName());
      smd.setObjectName(ObjectName.getInstance("test:service=C"));
      ServiceAttributeMetaData attrib = new ServiceAttributeMetaData();
      attrib.setName("A");
      attrib.setValue(new ServiceInjectionValueMetaData("A"));
      smd.addAttribute(attrib);
      Deployment bean = addJMX("jmxA", C.class, null, smd, A.class);
      try
      {
         ControllerContext context = getServiceContext("test:service=C", null);

         Bundle bundle1 = installBundle(assembleBundle("simple2", "/bundles/service/service-bundle3"));
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

               Object c = getService("test:service=C");
               assertSame(a, getter(c, "getA", "test:service=C"));

               ServiceReference ref1 = bundleContext1.getServiceReference(A.class.getName());
               assertUsingBundles(ref1, LazyBundle.getBundle(getDeploymentUnit(bean)));

               changeContext(context, ControllerState.DESCRIBED);
               // we did un-injection, should be removed now
               assertUsingBundles(ref1);

               changeContext(context, ControllerState.INSTALLED);
               assertEquals(ControllerState.INSTALLED, context.getState());
            }
            finally
            {
               reg1.unregister();
            }

            // check if the bean was unwinded as well
            assertEquals(ControllerState.INSTANTIATED, context.getState());
         }
         finally
         {
            uninstall(bundle1);
         }
      }
      finally
      {
         undeploy(bean);
      }
   }
   */
}