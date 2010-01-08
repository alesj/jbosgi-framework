/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.test.osgi;

// $Id: $

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;

import junit.framework.AssertionFailedError;

import org.jboss.classloader.plugins.ClassLoaderUtils;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.deployers.client.spi.DeployerClient;
import org.jboss.deployers.client.spi.Deployment;
import org.jboss.deployers.spi.attachments.MutableAttachments;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.deployers.structure.spi.main.MainDeployerStructure;
import org.jboss.deployers.vfs.spi.client.VFSDeployment;
import org.jboss.deployers.vfs.spi.client.VFSDeploymentFactory;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.AbstractDeployedBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.jboss.test.kernel.junit.MicrocontainerTestDelegate;
import org.jboss.virtual.AssembledDirectory;
import org.jboss.virtual.VFS;
import org.jboss.virtual.VirtualFile;
import org.jboss.virtual.plugins.context.jar.JarUtils;
import org.jboss.virtual.plugins.vfs.helpers.SuffixesExcludeFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * A OSGiTestDelegate
 * 
 * @author <a href="baileyje@gmail.com">John Bailey</a>
 * @author Thomas.Diesler@jboss.com
 */
public class FrameworkTestDelegate extends MicrocontainerTestDelegate
{
   private OSGiBundleManager bundleManager;

   /**
    * Create a new OSGiTestDelegate.
    * @param clazz test class
    * @throws Exception for any error
    */
   FrameworkTestDelegate(Class<?> clazz) throws Exception
   {
      super(clazz);
   }

   /**
    * Deploys the jboss-osgi-bootstrap.xml bean descriptor to setup OSGi deployers
    */
   protected void deploy() throws Exception
   {
      String common = "/bootstrap/jboss-osgi-bootstrap.xml";
      URL url = getClass().getResource(common);
      if (url == null)
         throw new IllegalStateException(common + " not found");
      deploy(url);
      try
      {
         super.deploy();
         deployBundles();
      }
      catch (Throwable t)
      {
         undeploy();
         if (t instanceof Exception)
            throw (Exception)t;
         if (t instanceof Error)
            throw (Error)t;
         throw new RuntimeException("Error during deploy", t);
      }
   }

   protected void undeploy()
   {
      undeployBundles();
      super.undeploy();
   }

   protected void deployBundles() throws Exception
   {
      try
      {
         Method method = clazz.getMethod("deployBundles", FrameworkTestDelegate.class);
         log.debug("Deploying Bundles...");
         method.invoke(null, this);
      }
      catch (NoSuchMethodException e)
      {
         log.debug("No deployBundles() in " + clazz.getName());
      }
   }

   protected void undeployBundles()
   {
      OSGiBundleManager bundleManager = getBundleManager();
      Collection<AbstractBundleState> bundles = bundleManager.getBundles();
      for (AbstractBundleState aux : bundles)
      {
         try
         {
            if (aux.getBundleId() != 0)
            {
               AbstractDeployedBundleState bundleState = (AbstractDeployedBundleState)aux;
               bundleManager.uninstallBundle(bundleState);
            }
         }
         catch (Throwable t)
         {
            getLog().warn("Error undeploying bundle: " + aux, t);
         }
      }
   }

   public OSGiBundleManager getBundleManager()
   {
      if (bundleManager == null)
      {
         bundleManager = getBean("OSGiBundleManager", ControllerState.INSTALLED, OSGiBundleManager.class);
         try
         {
            if (bundleManager.isFrameworkActive() == false)
               bundleManager.startFramework();
         }
         catch (BundleException ex)
         {
            throw new IllegalStateException("Cannot start bundle manager", ex);
         }
      }
      return bundleManager;
   }
   
   public DeployerClient getDeployerClient()
   {
      return getBundleManager().getDeployerClient();
   }

   /**
    * Create a bundle 
    * 
    * @param root the location to deploy
    * @param child the child to deploy
    * @return Bundle for the deployment
    * @throws Exception for any error
    */
   public Bundle addBundle(String root, String child) throws Exception
   {
      URL resourceRoot = getClass().getResource(root);
      if (resourceRoot == null)
         throw new AssertionFailedError("Resource not found: " + root);
      URL childResource = new URL(resourceRoot, child);
      AbstractBundleState bundleState = getBundleManager().installBundle(childResource);
      return bundleState.getBundleInternal();
   }

   /**
    * Create a bundle 
    *
    * @param file the virtual file
    * @return Bundle for the deployment
    * @throws Exception for any error
    */
   public Bundle addBundle(VirtualFile file) throws Exception
   {
      AbstractBundleState bundleState = getBundleManager().installBundle(file);
      return bundleState.getBundleInternal();
   }

   /**
    * Remove a bundle 
    * 
    * @param bundle the bundle to remove
    * @throws Exception for any error
    */
   public void uninstall(Bundle bundle) throws Exception
   {
      if (bundle.getState() != Bundle.UNINSTALLED)
      {
         AbstractDeployedBundleState bundleState = OSGiBundleState.assertBundleState(bundle);
         getBundleManager().uninstallBundle(bundleState);
      }
   }

   public DeploymentUnit getDeploymentUnit(Bundle bundle)
   {
      DeploymentUnit deploymentUnit = getBundleManager().getDeployment(bundle.getBundleId());
      if (deploymentUnit == null)
         throw new AssertionFailedError("Bundle not installed: " + bundle);
      return deploymentUnit;
   }

   public VirtualFile assembleBundle(String name, String resources, Class<?>... packages) throws Exception
   {
      return assembleBundle(name, new String[] { resources }, packages);
   }

   public VirtualFile assembleBundle(String name, String[] resourcePaths, Class<?>... packages) throws Exception
   {
      AssembledDirectory assembledDirectory = createAssembledDirectory(name, "", resourcePaths, packages);
      return assembledDirectory;
   }

   public Bundle deployBundle(String name, OSGiMetaData metaData, String resourcePath, Class<?>... packages) throws Exception
   {
      AssembledDirectory assembledDirectory = createAssembledDirectory(name, "", new String[] { resourcePath }, packages);
      return deployBundle(assembledDirectory, metaData);
   }

   public Bundle deployBundle(String name, OSGiMetaData metaData, String[] resourcePaths, Class<?>... packages) throws Exception
   {
      AssembledDirectory assembledDirectory = createAssembledDirectory(name, "", resourcePaths, packages);
      return deployBundle(assembledDirectory, metaData);
   }

   public Bundle deployBundle(VirtualFile virtualFile, OSGiMetaData metaData) throws Exception
   {
      Deployment deployment = createDeployment(virtualFile, metaData);
      return deployBundle(deployment);
   }

   public Bundle deployBundle(String root, String child) throws Exception
   {
      URL resourceRoot = getClass().getResource(root);
      if (resourceRoot == null)
         throw new AssertionFailedError("Resource not found: " + root);
      URL childResource = new URL(resourceRoot, child);
      return deployBundle(childResource);
   }

   public Bundle deployBundle(URL url) throws Exception
   {
      // Get the root file
      VirtualFile root;
      try
      {
         root = VFS.getRoot(url);
      }
      catch (IOException e)
      {
         throw new BundleException("Invalid url=" + url, e);
      }

      return deployBundle(root);
   }

   public Bundle deployBundle(VirtualFile file) throws Exception
   {
      return deployBundle(file, null);
   }
   
   public Bundle deployBundle(Deployment deployment) throws Exception
   {
      DeployerClient deployerClient = getDeployerClient();
      MainDeployerStructure deployerStructure = (MainDeployerStructure) deployerClient;
      deployerClient.deploy(deployment);
      try
      {
         DeploymentUnit unit = deployerStructure.getDeploymentUnit(deployment.getName());
         AbstractDeployedBundleState bundleState = unit.getAttachment(OSGiBundleState.class);
         if (bundleState == null)
            throw new IllegalStateException("Unable to determine bundle state for " + deployment.getName());

         return bundleState.getBundleInternal();
      }
      catch (Exception e)
      {
         deployerClient.undeploy(deployment);
         throw e;
      }
   }

   public Deployment createDeployment(VirtualFile virtualFile) throws Exception
   {
      return createDeployment(virtualFile, null);
   }

   public Deployment createDeployment(VirtualFile virtualFile, OSGiMetaData metaData) throws Exception
   {
      return createDeployment(virtualFile, metaData, OSGiMetaData.class);
   }

   @SuppressWarnings({"unchecked"})
   public <T> Deployment createDeployment(VirtualFile virtualFile, T metaData, Class<T> expectedType) throws Exception
   {
      VFSDeployment deployment = VFSDeploymentFactory.getInstance().createVFSDeployment(virtualFile);
      if (metaData != null)
      {
         if (expectedType == null)
            expectedType = (Class<T>)metaData.getClass();

         MutableAttachments att = (MutableAttachments)deployment.getPredeterminedManagedObjects();
         att.addAttachment(expectedType, metaData);
      }
      return deployment;
   }

   public AssembledDirectory createAssembledDirectory(String name, String rootName, String[] resourcePaths, Class<?>... packages) throws Exception
   {
      AssembledDirectory assembledDirectory = createAssembledDirectory(name, rootName);
      if (resourcePaths != null)
      {
         for (String path : resourcePaths)
            addPath(assembledDirectory, path, "");
      }
      if (packages != null)
      {
         for (Class<?> reference : packages)
            addPackage(assembledDirectory, reference);
      }
      return assembledDirectory;
   }

   public AssembledDirectory createAssembledDirectory(String name) throws Exception
   {
      return createAssembledDirectory(name, "");
   }

   public AssembledDirectory createAssembledDirectory(String name, String rootName) throws Exception
   {
      return AssembledDirectory.createAssembledDirectory(name, rootName);
   }

   public void addPackage(AssembledDirectory dir, Class<?> reference) throws Exception
   {
      String packagePath = ClassLoaderUtils.packageNameToPath(reference.getName());
      dir.addResources(reference, new String[] { packagePath + "/*.class" }, new String[0]);
   }

   public void addPath(final AssembledDirectory dir, String path, String name) throws Exception
   {
      URL url = getClass().getResource(path);
      if (url == null)
         throw new AssertionFailedError(path + " not found");

      VirtualFile file = VFS.getVirtualFile(url, name);
      // TODO - remove this filter after new VFS relase
      SuffixesExcludeFilter noJars = new SuffixesExcludeFilter(JarUtils.getSuffixes());
      dir.addPath(file, noJars);
   }

   public URL getBundleResource(Bundle bundle, String path)
   {
      return getDeploymentUnit(bundle).getResourceLoader().getResource(path);
   }

   public Enumeration<URL> getBundleResources(Bundle bundle, String path) throws Exception
   {
      return getDeploymentUnit(bundle).getResourceLoader().getResources(path);
   }
}
