/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
import org.jboss.deployers.spi.deployer.helpers.AbstractRealDeployer;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.osgi.framework.metadata.internal.AbstractOSGiMetaData;
import org.jboss.osgi.framework.plugins.StartLevelPlugin;
import org.osgi.framework.Version;

/**
 * @author <a href="david@redhat.com">David Bosschaert</a>
 */
public class OSGiStartLevelMetaDataDeployer extends AbstractRealDeployer
{
   private final OSGiBundleManager bundleManager;

   public OSGiStartLevelMetaDataDeployer(OSGiBundleManager bm)
   {
      bundleManager = bm;

      setInput(OSGiMetaData.class);
      setStage(DeploymentStages.POST_PARSE);
      setTopLevelOnly(true);
   }

   @Override
   protected void internalDeploy(DeploymentUnit unit) throws DeploymentException
   {
      StartLevelPlugin slp = bundleManager.getOptionalPlugin(StartLevelPlugin.class);
      if (slp == null)
         return;

      OSGiMetaData md = unit.getAttachment(OSGiMetaData.class);
      if (md instanceof AbstractOSGiMetaData)
      {
         AbstractOSGiMetaData amd = (AbstractOSGiMetaData)md;
         int bsl = slp.getInitialBundleStartLevel(
               md.getBundleSymbolicName(),
               Version.parseVersion(md.getBundleVersion()));

         if (bsl != StartLevelPlugin.INITIAL_BUNDLE_STARTLEVEL_UNSPECIFIED)
         {
            amd.setInitialStartLevel(bsl);
         }
      }
   }
}
