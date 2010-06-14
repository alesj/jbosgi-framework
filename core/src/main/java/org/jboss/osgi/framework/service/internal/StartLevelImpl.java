/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.osgi.framework.service.internal;

//$Id$

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.bundle.OSGiBundleState;
import org.jboss.osgi.framework.bundle.OSGiSystemState;
import org.jboss.osgi.framework.plugins.FrameworkEventsPlugin;
import org.jboss.osgi.framework.plugins.StartLevelPlugin;
import org.jboss.osgi.framework.plugins.internal.AbstractServicePlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.startlevel.StartLevel;

/**
 * An implementation of the {@link StartLevel} service.
 * 
 * @author thomas.diesler@jboss.com
 * @author <a href="david@redhat.com">David Bosschaert</a>
 * @since 31-Aug-2009
 */
public class StartLevelImpl extends AbstractServicePlugin implements StartLevelPlugin
{
   private static final Logger log = Logger.getLogger(StartLevelImpl.class);

   FrameworkEventsPlugin eventsPlugin;
   Executor executor = Executors.newSingleThreadExecutor();

   private int initialStartLevel = 1;
   private ServiceRegistration registration;
   private int startLevel = 0; // Guarded by this

   public StartLevelImpl(OSGiBundleManager bundleManager)
   {
      super(bundleManager);
      eventsPlugin = getPlugin(FrameworkEventsPlugin.class);

      String beginning = bundleManager.getProperty(Constants.FRAMEWORK_BEGINNING_STARTLEVEL);
      if (beginning != null)
      {
         try
         {
            initialStartLevel = Integer.parseInt(beginning);
         }
         catch (NumberFormatException nfe)
         {
            log.error("Could not set beginning start level to: '" + beginning + "'");
         }
      }
   }

   @Override
   public void startService()
   {
      registration = getSystemContext().registerService(StartLevel.class.getName(), this, null);
   }

   @Override
   public void stopService()
   {
      if (registration != null)
      {
         registration.unregister();
         registration = null;
      }
   }

   @Override
   public int getBundleStartLevel(Bundle bundle)
   {
      AbstractBundleState b = AbstractBundleState.assertBundleState(bundle);
      if (b instanceof OSGiSystemState)
         return 0;
      else if (b instanceof OSGiBundleState)
         return ((OSGiBundleState)b).getStartLevel();

      return 1;
   }

   @Override
   public int getInitialBundleStartLevel()
   {
      return initialStartLevel;
   }

   @Override
   public synchronized int getStartLevel()
   {
      return startLevel;
   }

   @Override
   public boolean isBundleActivationPolicyUsed(Bundle bundle)
   {
      return false;
   }

   @Override
   public boolean isBundlePersistentlyStarted(Bundle bundle)
   {
      AbstractBundleState bundleState = AbstractBundleState.assertBundleState(bundle);
      return bundleState.isPersistentlyStarted();
   }

   @Override
   public void setBundleStartLevel(Bundle bundle, int sl)
   {
      final OSGiBundleState obs = OSGiBundleState.assertBundleState(bundle);
      obs.setStartLevel(sl);

      if (sl <= getStartLevel())
      {
         log.info("Start Level Service about to start: " + obs);
         executor.execute(new Runnable()
         {
            @Override
            public void run()
            {
               log.info("Start Level Service starting: " + obs);
               try
               {
                  int opts = Bundle.START_TRANSIENT;
                  if (isBundleActivationPolicyUsed(obs))
                     opts |= Bundle.START_ACTIVATION_POLICY;

                  obs.start(opts);
               }
               catch (BundleException e)
               {
                  eventsPlugin.fireFrameworkEvent(obs, FrameworkEvent.ERROR, e);
               }
            }
         });
      }
      else
      {
         log.info("Start Level Service about to stop: " + obs);
         executor.execute(new Runnable()
         {
            @Override
            public void run()
            {
               log.info("Start Level Service stopping: " + obs);
               try
               {
                  obs.stop(Bundle.STOP_TRANSIENT);
               }
               catch (BundleException e)
               {
                  eventsPlugin.fireFrameworkEvent(obs, FrameworkEvent.ERROR, e);
               }
            }
         });
      }
   }

   @Override
   public void setInitialBundleStartLevel(int startlevel)
   {
      initialStartLevel  = startlevel;
   }

   @Override
   public synchronized void setStartLevel(final int sl)
   {
      if (sl > getStartLevel())
      {
         log.info("About to increase start level from " + getStartLevel() + " to " + sl);
         executor.execute(new Runnable()
         {            
            @Override
            public void run()
            {
               log.info("Increasing start level from " + getStartLevel() + " to " + sl);
               increaseStartLevel(sl);
               eventsPlugin.fireFrameworkEvent(getSystemContext().getBundle(), FrameworkEvent.STARTLEVEL_CHANGED, null);
            }
         });
      }
      else if (sl < getStartLevel())
      {
         log.info("About to decrease start level from " + getStartLevel() + " to " + sl);
         executor.execute(new Runnable()
         {            
            @Override
            public void run()
            {
               log.info("Decreasing start level from " + getStartLevel() + " to " + sl);
               decreaseStartLevel(sl);
               eventsPlugin.fireFrameworkEvent(getSystemContext().getBundle(), FrameworkEvent.STARTLEVEL_CHANGED, null);
            }
         });
      }
   }

   public synchronized void increaseStartLevel(int sl)
   {
      Collection<AbstractBundleState> bundles = getBundleManager().getBundles();
      while (startLevel < sl)
      {
         startLevel++;
         log.info("Starting bundles for start level " + startLevel);
         for (AbstractBundleState b : bundles)
         {
            if (!(b instanceof OSGiBundleState))
               continue;

            OSGiBundleState obs = (OSGiBundleState)b;
            if (obs.getStartLevel() == startLevel && obs.isPersistentlyStarted())
            {
               try
               {
                  int opts = Bundle.START_TRANSIENT;
                  if (isBundleActivationPolicyUsed(b))
                  {
                     opts |= Bundle.START_ACTIVATION_POLICY;
                  }
                  b.start(opts);
               }
               catch (Throwable e)
               {
                  eventsPlugin.fireFrameworkEvent(b, FrameworkEvent.ERROR, e);
               }
            }
         }
      }
   }

   synchronized void decreaseStartLevel(int sl)
   {
      Collection<AbstractBundleState> bundles = getBundleManager().getBundles();
      while (startLevel > sl)
      {
         log.info("Stopping bundles for start level " + startLevel);
         for (AbstractBundleState b : bundles)
         {
            if (!(b instanceof OSGiBundleState))
               continue;

            OSGiBundleState obs = (OSGiBundleState)b;
            if (obs.getStartLevel() == startLevel)
            {
               try
               {
                  b.stop(Bundle.STOP_TRANSIENT);
               }
               catch (Throwable e)
               {
                  eventsPlugin.fireFrameworkEvent(b, FrameworkEvent.ERROR, e);
               }
            }
         }
         startLevel--;
      }
   }
}