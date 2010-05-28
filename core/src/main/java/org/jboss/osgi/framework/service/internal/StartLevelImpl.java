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
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.startlevel.StartLevel;

/**
 * An implementation of the {@link StartLevel}.
 * 
 * [TODO] [JBOSGI-150] Fully implement StartLevel 
 * 
 * @author thomas.diesler@jboss.com
 * @author <a href="david@redhat.com">David Bosschaert</a>
 * @since 31-Aug-2009
 */
public class StartLevelImpl extends AbstractServicePlugin implements StartLevelPlugin
{
   /** The log */
   private static final Logger log = Logger.getLogger(StartLevelImpl.class);

   FrameworkEventsPlugin eventsPlugin;
   Executor executor = Executors.newSingleThreadExecutor();
   /* Executor executor = new Executor()
   {
      public void execute(Runnable command)
      {
         command.run();
      }
   }; */   
   private int initialStartLevel = 1;
   private ServiceRegistration registration;
   private int startLevel = 0;


   public StartLevelImpl(OSGiBundleManager bundleManager)
   {
      super(bundleManager);
      eventsPlugin = getPlugin(FrameworkEventsPlugin.class);
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
      if (bundle instanceof OSGiSystemState)
         return 0;

      OSGiBundleState obs = OSGiBundleState.assertBundleState(bundle);
      if (obs != null)
      {
         return obs.getStartLevel();
      }
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
      OSGiBundleState obs = OSGiBundleState.assertBundleState(bundle);
      if (obs != null)
      {
         return obs.isPersistentlyStarted();
      }
      return false;
   }

   @Override
   public void setBundleStartLevel(Bundle bundle, int sl)
   {
      final OSGiBundleState obs = OSGiBundleState.assertBundleState(bundle);
      obs.setStartLevel(sl);

      if (sl <= getStartLevel())
      {
         executor.execute(new Runnable()
         {
            @Override
            public void run()
            {
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
         executor.execute(new Runnable()
         {
            @Override
            public void run()
            {
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
      if (sl > startLevel)
      {
         log.info("Increasing start level from " + startLevel + " to " + sl);
         executor.execute(new Runnable()
         {            
            @Override
            public void run()
            {
               increaseStartLevel(sl);
               eventsPlugin.fireFrameworkEvent(getSystemContext().getBundle(), FrameworkEvent.STARTLEVEL_CHANGED, null);
            }
         });
      }
      else if (sl < startLevel)
      {
         log.info("Decreasing start level from " + startLevel + " to " + sl);
         executor.execute(new Runnable()
         {            
            @Override
            public void run()
            {
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