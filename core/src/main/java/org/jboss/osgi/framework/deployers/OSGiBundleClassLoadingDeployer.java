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

// $Id: $

import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.metadata.OSGiMetaData;

/**
 * An OSGi classloading deployer, that maps osgi metadata into classloading metadata
 * for non-fragment bundles.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 * @version $Revision: 1.1 $
 */
public class OSGiBundleClassLoadingDeployer extends AbstractOSGiClassLoadingDeployer
{
   @Override
   public void deploy(DeploymentUnit unit, OSGiMetaData osgiMetaData) throws DeploymentException
   {
      super.deploy(unit, osgiMetaData);
      
      // Return if this is not a real bundle (i.e. a fragment) 
      AbstractBundleState bundleState = unit.getAttachment(AbstractBundleState.class);
      if (bundleState.isFragment())
         return;
      
      // nothing special to do
   }
}