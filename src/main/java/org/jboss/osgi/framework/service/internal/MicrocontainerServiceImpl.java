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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.StandardMBean;

import org.jboss.dependency.plugins.AbstractController;
import org.jboss.dependency.plugins.AbstractControllerContext;
import org.jboss.dependency.plugins.AbstractControllerContextActions;
import org.jboss.dependency.plugins.action.ControllerContextAction;
import org.jboss.dependency.spi.ControllerContext;
import org.jboss.dependency.spi.ControllerContextActions;
import org.jboss.dependency.spi.ControllerState;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.spi.dependency.KernelController;
import org.jboss.kernel.spi.dependency.KernelControllerContext;
import org.jboss.logging.Logger;
import org.jboss.osgi.framework.bundle.OSGiBundleManager;
import org.jboss.osgi.framework.plugins.MicrocontainerServicePlugin;
import org.jboss.osgi.framework.plugins.internal.AbstractServicePlugin;
import org.jboss.osgi.spi.management.MicrocontainerServiceMBean;
import org.jboss.osgi.spi.service.MicrocontainerService;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * An implementation of the {@link MicrocontainerService}.
 * 
 * @author thomas.diesler@jboss.com
 * @since 31-Aug-2009
 */
public class MicrocontainerServiceImpl extends AbstractServicePlugin implements MicrocontainerServicePlugin, MicrocontainerServiceMBean
{
   /** The log */
   private static final Logger log = Logger.getLogger(MicrocontainerServiceImpl.class);

   private Kernel kernel;
   private ServiceRegistration registration;

   public MicrocontainerServiceImpl(OSGiBundleManager bundleManager)
   {
      super(bundleManager);

      this.kernel = bundleManager.getKernel();
   }

   public void startService()
   {
      installKernelBean(BEAN_BUNDLE_CONTEXT, getSystemContext());
      registration = getSystemContext().registerService(MicrocontainerService.class.getName(), this, null);

      // Track the MBeanServer and register this service as an MBean
      try
      {
         String filter = "(" + Constants.OBJECTCLASS + "=" + MBeanServer.class.getName() + ")";
         getSystemContext().addServiceListener(new JMXServiceListener(this), filter);
      }
      catch (InvalidSyntaxException ex)
      {
         // ignore
      }
   }

   public void stopService()
   {
      if (registration != null)
      {
         registration.unregister();
         registration = null;
      }
   }

   public Object getRegisteredBean(String beanName)
   {
      ControllerContext context = kernel.getController().getInstalledContext(beanName);
      return context != null ? context.getTarget() : null;
   }

   @SuppressWarnings("unchecked")
   public <T> T getRegisteredBean(Class<T> beanClass, String beanName)
   {
      Object target = getRegisteredBean(beanName);
      if (target == null | beanClass.isAssignableFrom(target.getClass()) == false)
         return null;

      return (T)target;
   }

   public List<String> getRegisteredBeans()
   {
      List<String> names = new ArrayList<String>();

      AbstractController controller = (AbstractController)kernel.getController();
      for (ControllerContext ctx : controller.getAllContexts())
      {
         if (ctx instanceof KernelControllerContext)
            names.add(ctx.getName().toString());
      }

      return names;
   }

   private void installKernelBean(String beanName, Object target)
   {
      KernelController controller = kernel.getController();
      ControllerContextActions actions = new AbstractControllerContextActions(new HashMap<ControllerState, ControllerContextAction>());
      try
      {
         controller.install(new AbstractControllerContext(beanName, actions, null, target));
      }
      catch (Throwable th)
      {
         throw new IllegalStateException("Cannot install kernel bean: " + beanName, th);
      }
   }

   private void registerMBeans(MBeanServer server, MicrocontainerServiceMBean mbeanImpl)
   {
      try
      {
         if (server != null)
         {
            installKernelBean(BEAN_MBEAN_SERVER, server);
            StandardMBean mbean = new StandardMBean(mbeanImpl, MicrocontainerServiceMBean.class);
            server.registerMBean(mbean, MBEAN_MICROCONTAINER_SERVICE);
         }
      }
      catch (Exception ex)
      {
         throw new IllegalStateException("Cannot register MicrocontainerServiceMBean", ex);
      }
   }

   private void unregisterMBeans(MBeanServer server)
   {
      try
      {
         if (server != null && server.isRegistered(MBEAN_MICROCONTAINER_SERVICE))
            server.unregisterMBean(MBEAN_MICROCONTAINER_SERVICE);
      }
      catch (Exception ex)
      {
         log.warn("Cannot unregister MicrocontainerServiceMBean", ex);
      }
   }

   class JMXServiceListener implements ServiceListener
   {
      private MicrocontainerServiceMBean mbean;

      public JMXServiceListener(MicrocontainerServiceMBean mbean)
      {
         this.mbean = mbean;
      }

      public void serviceChanged(ServiceEvent event)
      {
         ServiceReference sref = event.getServiceReference();
         MBeanServer server = (MBeanServer)getSystemContext().getService(sref);
         int type = event.getType();
         switch (type)
         {
            case ServiceEvent.REGISTERED:
               registerMBeans(server, mbean);
               break;
            case ServiceEvent.UNREGISTERING:
               unregisterMBeans(server);
               break;
         }
      }
   }
}