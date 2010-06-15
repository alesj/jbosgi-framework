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
package org.jboss.osgi.framework.classloading;

import java.util.List;

import org.jboss.classloader.spi.ClassLoaderPolicy;
import org.jboss.classloading.spi.dependency.Module;
import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.vfs.plugins.classloader.VFSDeploymentClassLoaderPolicyModule;
import org.jboss.deployers.vfs.spi.structure.VFSDeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.vfs.VirtualFile;

/**
 * The {@link Module} that represents and OSGi bundle deployment.
 * 
 * @author thomas.diesler@jboss.com
 * @version $Revision$
 */
public class OSGiModule extends VFSDeploymentClassLoaderPolicyModule
{
   private static final long serialVersionUID = 1L;

   // Provide logging
   private static final Logger log = Logger.getLogger(OSGiModule.class);
   
   public OSGiModule(DeploymentUnit unit, ClassLoadingMetaData metaData)
   {
      super(unit);
      if (log.isTraceEnabled())
         log.trace("new OSGiModule\n  " + unit + "\n  " + metaData);
   }

   @Override
   public ClassLoaderPolicy createClassLoaderPolicy()
   {
      VFSDeploymentUnit unit = (VFSDeploymentUnit)getDeploymentUnit();
      return createClassLoaderPolicyInternal(unit);
   }
   
   ClassLoaderPolicy createClassLoaderPolicyInternal(VFSDeploymentUnit unit)
   {
      AbstractBundleState bundleState = unit.getAttachment(AbstractBundleState.class);
      VirtualFile[] roots = getClassLoaderPolicyRoots(bundleState, unit);
      ClassLoaderPolicy policy = new OSGiClassLoaderPolicy(bundleState, roots);
      unit.addAttachment(ClassLoaderPolicy.class, policy);
      return policy;
   }

   private VirtualFile[] getClassLoaderPolicyRoots(AbstractBundleState bundleState, VFSDeploymentUnit vfsUnit)
   {
      // The classpath is initialised by the bundle structure deployer
      List<VirtualFile> classPaths = vfsUnit.getClassPath();
      VirtualFile[] policyRoots = new VirtualFile[classPaths.size()];
      classPaths.toArray(policyRoots);
      return policyRoots;
   }
}
