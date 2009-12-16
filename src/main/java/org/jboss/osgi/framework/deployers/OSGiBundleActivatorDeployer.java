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

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.osgi.framework.BundleException;

/**
 * OSGiBundleActivatorDeployer.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class OSGiBundleActivatorDeployer extends AbstractSimpleRealDeployer<OSGiBundleState>
{
   public OSGiBundleActivatorDeployer()
   {
      super(OSGiBundleState.class);
      setTopLevelOnly(true);
   }

   @Override
   public void deploy(DeploymentUnit unit, OSGiBundleState bundleState) throws DeploymentException
   {
      try
      {
         bundleState.startInternal();
      }
      catch (BundleException ex)
      {
         // We do not rethrow this exception to the deployer framework.
         // An exception during Bundle.start() is regarded as a normal deployment condition and handeled internally by the OSGi layer.
         // The OSGiBundleManager picks up this BundleException and rethrows it if available.
         unit.addAttachment(BundleException.class, ex);
      }
   }

   @Override
   public void undeploy(DeploymentUnit unit, OSGiBundleState bundleState)
   {
      try
      {
         bundleState.stopInternal();
      }
      catch (BundleException ex)
      {
         // We do not rethrow this exception to the deployer framework.
         // An exception during Bundle.start() is regarded as a normal deployment condition and handeled internally by the OSGi layer.
         // The OSGiBundleManager picks up this BundleException and rethrows it if available.
         unit.addAttachment(BundleException.class, ex);
      }
   }
}
