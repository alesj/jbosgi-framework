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
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.bundle.OSGiFragmentState;
import org.osgi.framework.Bundle;

/**
 * A deployer that sets the bundle state to RESOLVED when the ClassLoader becomes available.
 * 
 * @author thomas.diesler@jboss.com
 * @since 24-Feb-2010
 */
public class OSGiBundleStateResolveDeployer extends AbstractSimpleRealDeployer<OSGiBundleState>
{
   // The relative order at which to change the bindle state to RESOLVED
   static final int RELATIVE_ORDER = 200;
   
   public OSGiBundleStateResolveDeployer()
   {
      super(OSGiBundleState.class);
      setStage(DeploymentStages.CLASSLOADER);
      setRelativeOrder(RELATIVE_ORDER);
      setTopLevelOnly(true);
   }

   @Override
   public void deploy(DeploymentUnit unit, OSGiBundleState bundleState) throws DeploymentException
   {
      // Change the bundle state to RESOLVED
      bundleState.changeState(Bundle.RESOLVED);
      for (OSGiFragmentState fragment : bundleState.getAttachedFragments())
         fragment.changeState(Bundle.RESOLVED);
   }
}