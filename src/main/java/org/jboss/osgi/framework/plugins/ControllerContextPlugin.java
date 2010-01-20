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

import java.util.Set;

import org.jboss.dependency.spi.ControllerContext;
import org.jboss.deployers.structure.spi.DeploymentUnit;
import org.jboss.osgi.framework.bundle.AbstractBundleState;
import org.jboss.osgi.framework.bundle.AbstractDeployedBundleState;


/**
 * A plugin that manages kernel controller contexts.
 * 
 * @author thomas.diesler@jboss.com
 * @since 20-Jan-2010
 */
public interface ControllerContextPlugin extends Plugin
{
   /**
    * Put context to deployment mapping.
    *
    * @param context the context
    * @param unit the deployment
    * @return previous mapping value
    */
   DeploymentUnit putContext(ControllerContext context, DeploymentUnit unit);

   /**
    * Remove context to deployment mapping.
    *
    * @param context the context
    * @param unit the deployment
    * @return is previous mapping value same as unit param
    */
   DeploymentUnit removeContext(ControllerContext context, DeploymentUnit unit);

   /**
    * Get registered contexts for bundle.
    *
    * @param bundleState the owning bundle
    * @return registered contexts
    */
   Set<ControllerContext> getRegisteredContext(AbstractDeployedBundleState bundleState);

   /**
    * Unregister contexts.
    *
    * @param bundleState the stopping bundle
    */
   void unregisterContexts(AbstractDeployedBundleState bundleState);
   
   /**
    * Get bundle for user tracker.
    *
    * @param user the user tracker object
    * @return bundle state
    */
   AbstractBundleState getBundleForUser(Object user);
   
   /**
    * Get bundle for context.
    *
    * @param context the context
    * @return bundle state
    */
   AbstractBundleState getBundleForContext(ControllerContext context);
   
}