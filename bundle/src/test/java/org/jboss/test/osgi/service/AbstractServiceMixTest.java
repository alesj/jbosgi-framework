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

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;

import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.test.osgi.AbstractDeploymentTest;
import org.osgi.framework.Bundle;

/**
 * Test MC's services.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class AbstractServiceMixTest extends AbstractDeploymentTest
{
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

   protected Bundle getBundle(Deployment deployment) throws Exception
   {
      return getBundle(getDeploymentUnit(deployment));
   }

   protected Bundle getBundle(DeploymentUnit unit) throws Exception
   {
      AbstractBundleState bundle = unit.getAttachment(AbstractBundleState.class);
      assertNotNull(bundle);
      return bundle.getBundleInternal();
   }
}