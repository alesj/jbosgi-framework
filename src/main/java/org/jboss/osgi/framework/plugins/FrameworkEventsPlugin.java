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
package org.jboss.osgi.framework.plugins;

//$Id: SystemPackagesPlugin.java 92761 2009-08-24 22:10:03Z thomas.diesler@jboss.com $

import org.jboss.osgi.framework.bundle.OSGiServiceState;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceListener;

/**
 * A plugin that handles the various OSGi event types.  
 * 
 * @author thomas.diesler@jboss.com
 * @since 27-Aug-2009
 */
public interface FrameworkEventsPlugin extends Plugin
{
   void addBundleListener(Bundle bundle, BundleListener listener);

   void removeBundleListener(Bundle bundle, BundleListener listener);
   
   void removeBundleListeners(Bundle bundle);

   void addFrameworkListener(Bundle bundle, FrameworkListener listener);

   void removeFrameworkListener(Bundle bundle, FrameworkListener listener);

   void removeFrameworkListeners(Bundle bundle);

   void addServiceListener(Bundle bundle, ServiceListener listener, Filter filter);

   void removeServiceListener(Bundle bundle, ServiceListener listener);
   
   void removeServiceListeners(Bundle bundle);
   
   void fireBundleEvent(Bundle bundle, int type);

   void fireFrameworkEvent(Bundle bundle, int type, Throwable throwable);

   // [TODO] remove dependecy on propriatary API
   void fireServiceEvent(Bundle bundle, int type, OSGiServiceState service);
}