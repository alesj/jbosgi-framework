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

import org.jboss.dependency.spi.ControllerContext;
import org.jboss.osgi.framework.bundle.AbstractBundleState;


/**
 * A plugin that manages kernel controller contexts.
 * 
 * @author thomas.diesler@jboss.com
 * @since 20-Jan-2010
 */
public interface ControllerContextPlugin extends Plugin
{
   /**
    * Get bundle for user tracker.
    *
    * @param user the user tracker object
    * @return bundle state
    */
   AbstractBundleState getBundleForUser(Object user);
   
   /**
    * Get the bundle that provides the given controller context.
    * 
    * [TODO ServiceMix] describe the intension, not the implementation
    * 
    * If context corresponds to an OSGi service, return the bundle that 
    * registered the service.
    * 
    * If the context was provided by a non OSGi deployment, dynamically create an OSGi bundle
    * and register it with the {@link OSGiBundleManager}
    * 
    * If the context was not provided by a deployment, return the system bundle.
    */
   AbstractBundleState getBundleForContext(ControllerContext context);
   
}