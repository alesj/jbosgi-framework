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
package org.jboss.osgi.framework.bundle;

// $Id: $

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.classloading.spi.metadata.ClassLoadingMetaData;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.classloading.OSGiClassLoadingMetaData;
import org.jboss.osgi.framework.classloading.OSGiClassLoadingMetaData.FragmentHost;
import org.jboss.osgi.framework.metadata.OSGiMetaData;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

/**
 * The state of a user deployed {@link Bundle} and its associated {@link BundleContext}.
 * 
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 * @version $Revision: 1.1 $
 */
public class OSGiBundleState extends AbstractDeployedBundleState
{
   // The list of attached fragments
   private List<OSGiFragmentState> attachedFragments = new CopyOnWriteArrayList<OSGiFragmentState>();

   /**
    * Create a new BundleState.
    * 
    * @param unit the deployment unit
    * @throws IllegalArgumentException for a null parameter
    */
   public OSGiBundleState(DeploymentUnit unit)
   {
      super(unit);
   }

   /**
    * Assert that the given bundle is an instance of OSGiBundleState
    * @throws IllegalArgumentException if the given bundle is not an instance of OSGiBundleState
    */
   public static OSGiBundleState assertBundleState(Bundle bundle)
   {
      bundle = AbstractBundleState.assertBundleState(bundle);
      if (bundle instanceof OSGiBundleState == false)
         throw new IllegalArgumentException("Not an OSGiBundleState: " + bundle);

      return (OSGiBundleState)bundle;
   }

   public List<OSGiFragmentState> getAttachedFragments()
   {
      return Collections.unmodifiableList(attachedFragments);
   }

   public boolean isFragmentAttachable(OSGiFragmentState fragmentState)
   {
      String hostName = getSymbolicName();
      Version hostVersion = getVersion();

      FragmentHost fragmentHost = fragmentState.getFragmentHost();
      if (hostName.equals(fragmentHost.getSymbolicName()) == false)
         return false;

      Version version = fragmentHost.getBundleVersion();
      if (version != null && hostVersion.equals(version) == false)
         return false;

      return true;
   }

   public void attachFragment(OSGiFragmentState fragmentState)
   {
      DeploymentUnit unit = getDeploymentUnit();
      OSGiClassLoadingMetaData clMetaData = (OSGiClassLoadingMetaData)unit.getAttachment(ClassLoadingMetaData.class);
      if (clMetaData == null)
         throw new IllegalStateException("Cannot obtain ClassLoadingMetaData for: " + this);

      DeploymentUnit fragUnit = fragmentState.getDeploymentUnit();
      OSGiClassLoadingMetaData fragMetaData = (OSGiClassLoadingMetaData)fragUnit.getAttachment(ClassLoadingMetaData.class);
      if (fragMetaData == null)
         throw new IllegalStateException("Cannot obtain ClassLoadingMetaData for: " + fragmentState);

      log.debug("Attach " + fragmentState + " -> " + this);
      attachedFragments.add(fragmentState);

      // attach classloading metadata to the hosts classloading metadata
      clMetaData.attachedFragmentMetaData(fragMetaData);
   }

   public boolean isFragment()
   {
      return false;
   }

   protected Set<ControllerContext> getRegisteredContexts()
   {
      return getBundleManager().getRegisteredContext(this);
   }

   public Class<?> loadClass(String name) throws ClassNotFoundException
   {
      checkInstalled();
      checkAdminPermission(AdminPermission.CLASS);

      if (resolveBundle() == false)
         throw new ClassNotFoundException("Cannot load class: " + name);

      ClassLoader classLoader = getDeploymentUnit().getClassLoader();
      return classLoader.loadClass(name);
   }

   public URL getResource(String name)
   {
      checkInstalled();
      if (noAdminPermission(AdminPermission.RESOURCE))
         return null;

      if (resolveBundle() == false)
         return getDeploymentUnit().getResourceLoader().getResource(name);

      return getDeploymentUnit().getClassLoader().getResource(name);
   }

   @SuppressWarnings("rawtypes")
   public Enumeration getResources(String name) throws IOException
   {
      checkInstalled();
      if (noAdminPermission(AdminPermission.RESOURCE))
         return null;

      if (resolveBundle() == false)
         return getDeploymentUnit().getResourceLoader().getResources(name);

      return getDeploymentUnit().getClassLoader().getResources(name);
   }

   // [TODO] options
   public void start(int options) throws BundleException
   {
      checkInstalled();
      checkAdminPermission(AdminPermission.EXECUTE);

      getBundleManager().startBundle(this);
   }

   public void stop(int options) throws BundleException
   {
      checkInstalled();
      checkAdminPermission(AdminPermission.EXECUTE);

      getBundleManager().stopBundle(this);
   }

   /**
    * Start internal.
    * 
    * This method is triggered by the OSGiBundleActivatorDeployer.
    * Preconditions are handled in OSGiBundleManager.startBundle()
    */
   public void startInternal() throws BundleException
   {
      // This bundle's state is set to STARTING
      // A bundle event of type BundleEvent.STARTING is fired
      createBundleContext();
      changeState(STARTING);

      // The BundleActivator.start(org.osgi.framework.BundleContext) method of this bundle's BundleActivator, if one is specified, is called. 
      try
      {
         OSGiMetaData osgiMetaData = getOSGiMetaData();
         if (osgiMetaData == null)
            throw new IllegalStateException("Cannot obtain OSGi meta data");

         // Do we have a bundle activator
         String bundleActivatorClassName = osgiMetaData.getBundleActivator();
         if (bundleActivatorClassName != null)
         {
            Object result = loadClass(bundleActivatorClassName).newInstance();
            if (result instanceof BundleActivator == false)
               throw new BundleException(bundleActivatorClassName + " is not an implementation of " + BundleActivator.class.getName());

            // Attach so we can call BundleActivator.stop() on this instance
            BundleActivator bundleActivator = (BundleActivator)result;
            getDeploymentUnit().addAttachment(BundleActivator.class, bundleActivator);

            bundleActivator.start(getBundleContext());
         }

         if (getState() != STARTING)
            throw new BundleException("Bundle has been uninstalled: " + this);

         changeState(ACTIVE);
      }

      // If the BundleActivator is invalid or throws an exception then:
      //   * This bundle's state is set to STOPPING.
      //   * A bundle event of type BundleEvent.STOPPING is fired.
      //   * Any services registered by this bundle must be unregistered.
      //   * Any services used by this bundle must be released.
      //   * Any listeners registered by this bundle must be removed.
      //   * This bundle's state is set to RESOLVED.
      //   * A bundle event of type BundleEvent.STOPPED is fired.
      //   * A BundleException is then thrown.
      catch (Throwable t)
      {
         // This bundle's state is set to STOPPING
         // A bundle event of type BundleEvent.STOPPING is fired
         changeState(STOPPING);

         // Any services registered by this bundle must be unregistered.
         // Any services used by this bundle must be released.
         // Any listeners registered by this bundle must be removed.
         stopInternal();

         // This bundle's state is set to RESOLVED
         // A bundle event of type BundleEvent.STOPPED is fired
         destroyBundleContext();
         changeState(RESOLVED);

         if (t instanceof BundleException)
            throw (BundleException)t;

         throw new BundleException("Cannot start bundle: " + this, t);
      }
   }

   /**
    * Stop Internal.
    * 
    * This method is triggered by the OSGiBundleActivatorDeployer.
    * Preconditions are handled in OSGiBundleManager.stopBundle()
    */
   public void stopInternal() throws BundleException
   {
      // This bundle's state is set to STOPPING
      // A bundle event of type BundleEvent.STOPPING is fired
      int priorState = getState();
      changeState(STOPPING);

      // If this bundle's state was ACTIVE prior to setting the state to STOPPING, 
      // the BundleActivator.stop(org.osgi.framework.BundleContext) method of this bundle's BundleActivator, if one is specified, is called. 
      // If that method throws an exception, this method must continue to stop this bundle and a BundleException must be thrown after completion 
      // of the remaining steps.
      Throwable rethrow = null;
      if (priorState == Bundle.ACTIVE)
      {
         BundleActivator bundleActivator = getDeploymentUnit().getAttachment(BundleActivator.class);
         BundleContext bundleContext = getBundleContext();
         if (bundleActivator != null && bundleContext != null)
         {
            try
            {
               bundleActivator.stop(bundleContext);
            }
            catch (Throwable t)
            {
               rethrow = t;
            }
         }
      }

      // Any services registered by this bundle must be unregistered
      getBundleManager().unregisterContexts(this);

      // Any services used by this bundle must be released
      for (ControllerContext context : getUsedContexts(this))
      {
         int count = getUsedByCount(context, this);
         while (count > 0)
         {
            try
            {
               getBundleManager().ungetContext(this, context);
            }
            catch (Throwable t)
            {
               log.debug("Error ungetting service: " + context, t);
            }
            count--;
         }
      }

      // [TODO] Any listeners registered by this bundle must be removed

      // If this bundle's state is UNINSTALLED, because this bundle was uninstalled while the 
      // BundleActivator.stop method was running, a BundleException must be thrown
      if (getState() == Bundle.UNINSTALLED)
         throw new BundleException("Bundle uninstalled during activator stop: " + this);

      // This bundle's state is set to RESOLVED
      // A bundle event of type BundleEvent.STOPPED is fired
      destroyBundleContext();
      changeState(RESOLVED);

      if (rethrow != null)
         throw new BundleException("Error during stop of bundle: " + this, rethrow);
   }

   /**
    * Updates this bundle. 
    * 
    * This method performs the same function as calling update(InputStream) with a null InputStream
    */
   public void update() throws BundleException
   {
      update(null);
   }

   /**
    * Updates this bundle from an InputStream. 
    */
   public void update(InputStream in) throws BundleException
   {
      checkAdminPermission(AdminPermission.LIFECYCLE);
      try
      {
         getBundleManager().updateBundle(this, in);
      }
      catch (Exception ex)
      {
         if (ex instanceof BundleException)
            throw (BundleException)ex;

         throw new BundleException("Cannot update bundle: " + this, ex);
      }
   }

   @Override
   public Dictionary<String, String> getHeaders(String locale)
   {
      // This method must continue to return Manifest header information while this bundle is in the UNINSTALLED state, 
      // however the header values must only be available in the raw and default locale values
      if (getState() == Bundle.UNINSTALLED)
         return headersOnUninstall;

      return super.getHeaders(locale);
   }

   @Override
   protected void afterServiceRegistration(OSGiServiceState service)
   {
      getBundleManager().putContext(service, getDeploymentUnit());
   }

   @Override
   protected void beforeServiceUnregistration(OSGiServiceState service)
   {
      getBundleManager().removeContext(service, getDeploymentUnit());
   }
}
