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

import junit.framework.Test;
import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.test.osgi.DeployersTest;
import org.jboss.test.osgi.service.support.LazyBundle;
import org.jboss.test.osgi.service.support.a.A;
import org.jboss.test.osgi.service.support.c.C;
import org.jboss.test.osgi.service.support.d.D;
import org.jboss.virtual.AssembledDirectory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/**
 * Test MC's services.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class ServicesTest extends DeployersTest
{
   public ServicesTest(String name)
   {
      super(name);
   }

   protected static Object getter(Object target, String getter, String name) throws Throwable
   {
      assertNotNull("Target " + name + " is not null", target);
      Class<?> clazz = target.getClass();
      Method m = clazz.getDeclaredMethod(getter);
      return m.invoke(target);
   }

   protected static Object setter(Object target, String setter, Object value, String name) throws Throwable
   {
      assertNotNull("Target " + name + " is not null", target);
      assertNotNull("Value is not null", value);
      Class<?> clazz = target.getClass();
      Method m = clazz.getDeclaredMethod(setter, value.getClass());
      return m.invoke(target, value);
   }
}