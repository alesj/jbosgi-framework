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
package org.jboss.osgi.framework.deployers;

// $Id: AbstractOSGiClassLoadingDeployer.java 101391 2010-02-24 12:58:50Z thomas.diesler@jboss.com $

import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.vfs.VirtualFile;

/**
 * The AbstractDeployment delegates to the jboss-vfs specific {@link DeploymentAdaptor}.
 * 
 * @author thomas.diesler@jboss.com
 * @since 03-Mar-2010
 */
public abstract class AbstractDeployment
{
   private static DeploymentAdaptor adaptor;

   public static Deployment createDeployment(VirtualFile root)
   {
      return getDeploymentAdaptor().createDeployment(root);
   }

   public static VirtualFile getRoot(DeploymentUnit unit)
   {
      return getDeploymentAdaptor().getRoot(unit);
   }
   
   @SuppressWarnings("unchecked")
   private static DeploymentAdaptor getDeploymentAdaptor()
   {
      if (adaptor == null)
      {
         try
         {
            String classname = "org.jboss.osgi.framework.deployers.DeploymentAdaptor21";
            ClassLoader classLoader = AbstractDeployment.class.getClassLoader();
            Class<DeploymentAdaptor> clazz = (Class<DeploymentAdaptor>)classLoader.loadClass(classname);
            adaptor = clazz.newInstance();
         }
         catch (Exception e)
         {
            // ignore
         }

         if (adaptor == null)
         {
            try
            {
               String classname = "org.jboss.osgi.framework.deployers.DeploymentAdaptor30";
               ClassLoader classLoader = AbstractDeployment.class.getClassLoader();
               Class<DeploymentAdaptor> clazz = (Class<DeploymentAdaptor>)classLoader.loadClass(classname);
               adaptor = clazz.newInstance();
            }
            catch (Exception e)
            {
               // ignore
            }
         }
         
         if (adaptor == null)
            throw new IllegalStateException("Cannot load DeploymentAdaptor");
      }
      return adaptor;
   }
}
