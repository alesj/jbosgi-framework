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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.deployers.client.spi.main.MainDeployer;
import org.jboss.deployers.plugins.main.MainDeployerImpl;
import org.jboss.deployers.spi.DeploymentException;
import org.jboss.deployers.spi.deployer.Deployers;
import org.jboss.deployers.spi.deployer.DeploymentStage;
import org.jboss.deployers.spi.deployer.DeploymentStages;
import org.jboss.deployers.structure.spi.DeploymentContext;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.logging.Logger;
import org.jboss.managed.api.ManagedObject;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.DeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * A Deployers implementation that wraps the deployers that are associated with the MainDeployer.
 * 
 * @author thomas.diesler@jboss.com
 * @since 04-Sep-2009
 */
public class OSGiDeployersWrapper implements Deployers
{
   /** The log */
   private static final Logger log = Logger.getLogger(OSGiDeployersWrapper.class);
   
   private MainDeployer mainDeployer;
   private Deployers deployers;
   private OSGiBundleManager bundleManager;
   private PackageAdmin packageAdmin;

   /** The list of unresolved bundles */
   private List<OSGiBundleState> unresolvedBundles = new CopyOnWriteArrayList<OSGiBundleState>();

   public OSGiDeployersWrapper(MainDeployer mainDeployer, OSGiBundleManager bundleManager)
   {
      if (bundleManager == null)
         throw new IllegalArgumentException("Null bundleManager");
      if (mainDeployer instanceof MainDeployerImpl == false)
         throw new IllegalStateException("Cannot instrument: " + mainDeployer);

      this.mainDeployer = mainDeployer;
      this.bundleManager = bundleManager;

      // Swap the deployers implementation 
      MainDeployerImpl mainDeployerImpl = (MainDeployerImpl)mainDeployer;
      this.deployers = mainDeployerImpl.getDeployers();
      mainDeployerImpl.setDeployers(this);
   }

   private PackageAdmin getPackageAdmin()
   {
      if (packageAdmin == null)
      {
         BundleContext sysContext = bundleManager.getSystemContext();
         ServiceReference sref = sysContext.getServiceReference(PackageAdmin.class.getName());
         if (sref == null)
            throw new IllegalStateException("Cannot obtain PackageAdmin");
         
         packageAdmin = (PackageAdmin)sysContext.getService(sref);
      }
      return packageAdmin;
   }

   public void process(List<DeploymentContext> deploy, List<DeploymentContext> undeploy)
   {
      // Delegate to the original deployers
      deployers.process(deploy, undeploy);

      // OSGi bundles resolve phase 
      afterDeployersProcess(deploy, undeploy);
   }

   private void afterDeployersProcess(List<DeploymentContext> deploy, List<DeploymentContext> undeploy)
   {
      // Process undeploy contexts
      if (undeploy != null)
      {
         for (DeploymentContext context : undeploy)
         {
            DeploymentUnit unit = context.getDeploymentUnit();
            AbstractBundleState bundle = unit.getAttachment(AbstractBundleState.class);
            if (bundle != null)
            {
               unresolvedBundles.remove(bundle);
            }
         }
      }
      
      // Process deploy contexts
      if (deploy != null)
      {
         // Collect unresolved bundles
         for (DeploymentContext context : deploy)
         {
            DeploymentUnit unit = context.getDeploymentUnit();
            AbstractBundleState bundle = unit.getAttachment(AbstractBundleState.class);
            if (bundle == null || bundle.isFragment())
               continue;
            
            Deployment dep = unit.getAttachment(Deployment.class);
            boolean autoStart = (dep != null ? dep.isAutoStart() : true);
            
            if (autoStart == true && bundle.getState() == Bundle.INSTALLED)
            {
               unresolvedBundles.add(0, (OSGiBundleState)bundle);
            }
         }
         
         // Try to resolve all unresolved bundles
         if (unresolvedBundles.isEmpty() == false)
         {
            OSGiBundleState[] unresolved = new OSGiBundleState[unresolvedBundles.size()];
            unresolvedBundles.toArray(unresolved);

            // Use PackageAdmin to resolve the bundles
            getPackageAdmin().resolveBundles(unresolved);
               
            for (DeployedBundleState aux : unresolved)
            {
               if (aux.getState() != Bundle.RESOLVED)
                  log.info("Unresolved: " + aux);
               
               if (aux.getState() == Bundle.RESOLVED)
               {
                  unresolvedBundles.remove(aux);
                  
                  try
                  {
                     // When resolved progress to INSTALLED
                     String name = aux.getDeploymentUnit().getName();
                     mainDeployer.change(name, DeploymentStages.INSTALLED);
                  }
                  catch (DeploymentException ex)
                  {
                     log.error(ex);
                  }
               }
            }
         }
      }
   }

   public void change(DeploymentContext context, DeploymentStage stage) throws DeploymentException
   {
      deployers.change(context, stage);
   }

   public void checkComplete(DeploymentContext... contexts) throws DeploymentException
   {
      deployers.checkComplete(contexts);
   }

   public void checkComplete(Collection<DeploymentContext> errors, Collection<org.jboss.deployers.client.spi.Deployment> missingDeployer) throws DeploymentException
   {
      deployers.checkComplete(errors, missingDeployer);
   }

   public void checkStructureComplete(DeploymentContext... contexts) throws DeploymentException
   {
      deployers.checkStructureComplete(contexts);
   }

   public DeploymentStage getDeploymentStage(DeploymentContext context) throws DeploymentException
   {
      return deployers.getDeploymentStage(context);
   }

   public Map<String, ManagedObject> getManagedObjects(DeploymentContext context) throws DeploymentException
   {
      return deployers.getManagedObjects(context);
   }

   public void shutdown()
   {
      deployers.shutdown();
   }
}
