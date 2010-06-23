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

import org.osgi.framework.Version;
import org.osgi.service.startlevel.StartLevel;

/**
 * The StartLevel service plugin
 * 
 * @author thomas.diesler@jboss.com
 * @author <a href="david@redhat.com">David Bosschaert</a>
 * @since 07-Sep-2009
 */
public interface StartLevelPlugin extends ServicePlugin, StartLevel
{
   public static final int INITIAL_BUNDLE_STARTLEVEL_UNSPECIFIED = -1;

   /**
    * Increase the start level to the specified level. 
    * This method moves to the specified start level in the current thread and
    * returns when the desired start level has been reached.
    * @param level the target start level.
    */
   void increaseStartLevel(int level);

   /**
    * Decrease the start level to the specified level.
    * This method moves to the specified start level in the current thread and
    * returns when the desired start level has been reached.
    * @param level the target start level.
    */
   void decreaseStartLevel(int level);

   /**
    * Returns the initial bundle start level for the specified bundle. This information
    * is provided at deploy time in configuration.
    * @param bsn the Bundle Symbolic Name. This is a required parameter.
    * @param version the version of the bundle. This is a required parameter, however 
    * it is ignored when in the configuration only the BSN is provided with a start level.
    * In other words, associating a start level in configuration with a BSN without a 
    * version applies that start level to all versions of bundles with that BSN. 
    * @return The initial start level for the specified bundle, or
    * {@link #INITIAL_BUNDLE_STARTLEVEL_UNSPECIFIED} if no initial start
    * level is defined for the specified bundle.
    */
   int getInitialBundleStartLevel(String bsn, Version version);
}