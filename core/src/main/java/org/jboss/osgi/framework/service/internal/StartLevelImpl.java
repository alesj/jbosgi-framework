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

import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.plugins.StartLevelPlugin;
import org.jboss.osgi.framework.plugins.internal.AbstractServicePlugin;
import org.osgi.framework.Bundle;
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

   private int initialStartLevel = 1;
   private ServiceRegistration registration;

   public StartLevelImpl(OSGiBundleManager bundleManager)
   {
      super(bundleManager);
   }

   public void startService()
   {
      registration = getSystemContext().registerService(StartLevel.class.getName(), this, null);
   }

   public void stopService()
   {
      if (registration != null)
      {
         registration.unregister();
         registration = null;
      }
   }

   public int getBundleStartLevel(Bundle bundle)
   {
      return 1;
   }

   public int getInitialBundleStartLevel()
   {
      return initialStartLevel;
   }

   public int getStartLevel()
   {
      return 1;
   }

   public boolean isBundleActivationPolicyUsed(Bundle bundle)
   {
      return false;
   }

   public boolean isBundlePersistentlyStarted(Bundle bundle)
   {
      return false;
   }

   public void setBundleStartLevel(Bundle bundle, int startlevel)
   {
      log.info("Ignore setBundleStartLevel(" + bundle + "," + startlevel + ")");
   }

   public void setInitialBundleStartLevel(int startlevel)
   {
      initialStartLevel  = startlevel;
   }

   public void setStartLevel(int startlevel)
   {
      log.info("Ignore setStartLevel(" + startlevel + ")");
   }
}