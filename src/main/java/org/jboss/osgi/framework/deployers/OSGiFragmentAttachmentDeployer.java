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

// $Id$

import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.spi.deployer.helpers.AbstractSimpleRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.bundle.OSGiFragmentState;
import org.osgi.framework.Bundle;

/**
 * A deployer, that handles OSGi fragment attachments.
 * 
 * Fragments are bundles that can be attached to one or more host bundles by the
 * Framework. Attaching is done as part of resolving: the Framework appends
 * the relevant definitions of the fragment bundles to the host’s definitions
 * before the host is resolved. 
 * 
 * @author Thomas.Diesler@jboss.com
 * @since 08-Jan-2010
 */
public class OSGiFragmentAttachmentDeployer extends AbstractSimpleRealDeployer<ClassLoadingMetaData>
{
   public OSGiFragmentAttachmentDeployer()
   {
      super(ClassLoadingMetaData.class);
      setStage(DeploymentStages.CLASSLOADER);
      setTopLevelOnly(true);
   }

   @Override
   public void deploy(DeploymentUnit unit, ClassLoadingMetaData classLoadingMetaData) throws DeploymentException
   {
      // Return if this is not a real bundle (i.e. a fragment) 
      AbstractBundleState absBundleState = unit.getAttachment(AbstractBundleState.class);
      if (absBundleState == null || absBundleState.isFragment())
         return;
      
      OSGiBundleState bundleState = (OSGiBundleState)absBundleState;
      
      // Iterate over all installed fragments and attach when appropriate 
      OSGiBundleManager bundleManager = bundleState.getBundleManager();
      for (AbstractBundleState auxBundle : bundleManager.getBundles(Bundle.INSTALLED))
      {
         if (auxBundle.isFragment())
         {
            OSGiFragmentState auxState = (OSGiFragmentState)auxBundle;
            if (bundleState.isFragmentAttachable(auxState))
               bundleState.attachFragment(auxState);
         }
      }
   }
}