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

   public void testMBeans() throws Throwable
   {
      // mix mbean services with bundles -- TODO
   }
}