/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
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
package org.jboss.test.osgi;

import org.jboss.beans.metadata.spi.BeanMetaData;
import org.jboss.beans.metadata.spi.builder.BeanMetaDataBuilder;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.virtual.AssembledDirectory;
import org.jboss.virtual.VirtualFile;
import org.jboss.osgi.framework.bundle.AbstractDeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.osgi.framework.Bundle;

/**
 * Deployers test - generic deployment test.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public abstract class DeployersTest extends FrameworkTest
{
   protected DeployersTest(String name)
   {
      super(name);
   }

   public static DeployersTestDelegate getDelegate(Class<?> clazz) throws Exception
   {
      return new DeployersTestDelegate(clazz);
   }

   protected DeployersTestDelegate getDelegate()
   {
      return (DeployersTestDelegate)super.getDelegate();
   }

   protected void checkComplete() throws Exception
   {
      getDelegate().checkComplete();
   }

   protected Deployment addDeployment(VirtualFile file) throws Exception
   {
      return getDelegate().addDeployment(file);
   }

   protected <T> Deployment addDeployment(VirtualFile file, T metadata, Class<T> expectedType) throws Exception
   {
      return getDelegate().addDeployment(file, metadata, expectedType);
   }

   protected Deployment assertDeploy(VirtualFile file) throws Exception
   {
      return getDelegate().assertDeploy(file);
   }

   protected <T> Deployment assertDeploy(VirtualFile file, T metadata, Class<T> expectedType) throws Exception
   {
      return getDelegate().assertDeploy(file, metadata, expectedType);
   }

   protected DeploymentUnit getDeploymentUnit(Deployment deployment) throws Exception
   {
      return getDelegate().getDeploymentUnit(deployment);
   }

   protected void undeploy(Deployment deployment) throws Exception
   {
      getDelegate().undeploy(deployment);
   }

   protected Deployment addBean(String name, Class<?> beanClass, Class<?> ... references) throws Exception
   {
      return addBean(name, beanClass, null, references);
   }

   protected Deployment addBean(String name, Class<?> beanClass, BeanMetaData bmd, Class<?> ... references) throws Exception
   {
      AssembledDirectory dir = createAssembledDirectory(name, "");
      if (beanClass != null)
         addPackage(dir, beanClass);
      if (references != null)
      {
         for (Class<?> reference : references)
            addPackage(dir, reference);
      }
      if (bmd == null)
      {
         BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(beanClass.getSimpleName(), beanClass.getName());
         bmd = builder.getBeanMetaData();
      }
      return addDeployment(dir, bmd, BeanMetaData.class);
   }

   protected Deployment deployBean(String name, Class<?> beanClass, Class<?> ... references) throws Exception
   {
      return deployBean(name, beanClass, null, references);
   }

   protected Deployment deployBean(String name, Class<?> beanClass, BeanMetaData bmd, Class<?> ... references) throws Exception
   {
      AssembledDirectory dir = createAssembledDirectory(name, "");
      addPackage(dir, beanClass);
      if (references != null)
      {
         for (Class<?> reference : references)
            addPackage(dir, reference);
      }
      if (bmd == null)
      {
         BeanMetaDataBuilder builder = BeanMetaDataBuilder.createBuilder(beanClass.getSimpleName(), beanClass.getName());
         bmd = builder.getBeanMetaData();
      }
      return assertDeploy(dir, bmd, BeanMetaData.class);
   }

   protected Bundle getBundle(Deployment deployment) throws Exception
   {
      return getBundle(getDeploymentUnit(deployment));
   }

   protected Bundle getBundle(DeploymentUnit unit) throws Exception
   {
      AbstractDeployedBundleState bundle = unit.getAttachment(OSGiBundleState.class);
      assertNotNull(bundle);
      return bundle.getBundleInternal();
   }
}