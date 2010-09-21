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
package org.jboss.osgi.framework.plugin;

import org.osgi.service.startlevel.StartLevel;

/**
 * @author <a href="david@redhat.com">David Bosschaert</a>
 */
public interface StartLevelPlugin extends Plugin, StartLevel
{
   static final int BUNDLE_STARTLEVEL_UNSPECIFIED = -1;

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
    * This method does the same as {@link StartLevel#setStartLevel(int)}
    * with an additional parameter to indicate whether this is triggered from 
    * framework start. The additional parameter only influences the event sent 
    * when the start level change is finished. 
    * @param startLevel The requested start level for the framework.
    * @param frameworkStart <tt>true</tt> if this start level change is initiated
    *  from a framework start operation. If so the Start Level Plugin with emit a
    *  FrameworkEvent.STARTED event when finished, otherwise it will emit a 
    *  FrameworkEvent.STARTLEVEL_CHANGED event.
    */
   void setStartLevel(int startLevel, boolean frameworkStart);
}
