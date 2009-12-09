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

import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.structure.spi.main.MainDeployerStructure;
import org.jboss.virtual.VirtualFile;

/**
 * Deployers test - generic deployment test delegate.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class DeployersTestDelegate extends FrameworkTestDelegate
{
   public DeployersTestDelegate(Class<?> clazz) throws Exception
   {
      super(clazz);
   }

   public void checkComplete() throws Exception
   {
      getDeployerClient().checkComplete();
   }

   public Deployment addDeployment(VirtualFile file) throws Exception
   {
      return addDeployment(file, null, null);
   }

   public <T> Deployment addDeployment(VirtualFile file, T metadata, Class<T> expectedType) throws Exception
   {
      Deployment deployment = createDeployment(file, metadata, expectedType);
      DeployerClient deployerClient = getDeployerClient();
      deployerClient.addDeployment(deployment);
      deployerClient.process();
      return deployment; 
   }

   public Deployment assertDeploy(VirtualFile file) throws Exception
   {
      return assertDeploy(file, null, null);
   }

   public <T> Deployment assertDeploy(VirtualFile file, T metadata, Class<T> expectedType) throws Exception
   {
      Deployment deployment = createDeployment(file, metadata, expectedType);
      DeployerClient deployerClient = getDeployerClient();
      deployerClient.deploy(deployment);
      return deployment;
   }

   public DeploymentUnit getDeploymentUnit(Deployment deployment) throws Exception
   {
      DeployerClient deployerClient = getDeployerClient();
      MainDeployerStructure deployerStructure = (MainDeployerStructure) deployerClient;
      return deployerStructure.getDeploymentUnit(deployment.getName());
   }

   public void undeploy(Deployment deployment) throws Exception
   {
      DeployerClient deployerClient = getDeployerClient();
      deployerClient.undeploy(deployment);
   }
}