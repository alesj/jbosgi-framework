package org.jboss.test.osgi.service.support;

import java.util.Properties;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.jboss.beans.metadata.api.annotations.Constructor;
import org.jboss.beans.metadata.api.annotations.Inject;
import org.jboss.classloader.spi.ClassLoaderSystem;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.plugins.bootstrap.basic.KernelConstants;
import org.jboss.system.ServiceController;

/**
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 */
public class JMXKernel
{
   private static final ObjectName CLASSLOADER_SYSTEM_OBJECT_NAME;

   private Kernel kernel;
   private ClassLoaderSystem system;

   private Properties properties;
   private MBeanServer mbeanServer;
   private boolean createdMBeanServer;
   private ServiceController serviceController;

   static
   {
      try
      {
         CLASSLOADER_SYSTEM_OBJECT_NAME = new ObjectName("jboss.classloader:service=ClassLoaderSystem");
      }
      catch (MalformedObjectNameException e)
      {
         throw new RuntimeException("Unexpected error", e);
      }
   }

   @Constructor
   public JMXKernel(@Inject(bean = KernelConstants.KERNEL_NAME) Kernel kernel, @Inject ClassLoaderSystem system)
   {
      this.kernel = kernel;
      this.system = system;
   }

   protected void addProperties()
   {
      if (properties != null)
         System.setProperties(properties);
   }

   protected void removeProperties()
   {
      if (properties != null)
      {
         for (Object key : properties.keySet())
            System.clearProperty(key.toString());
      }
   }

   public void create()
   {
      addProperties();

      if (mbeanServer == null)
      {
         mbeanServer = MBeanServerFactory.createMBeanServer("jboss");
         createdMBeanServer = true;
      }
   }

   public void start() throws Throwable
   {
      mbeanServer.registerMBean(system, CLASSLOADER_SYSTEM_OBJECT_NAME);

      serviceController = new ServiceController();
      serviceController.setKernel(kernel);
      serviceController.setMBeanServer(mbeanServer);
   }

   public void stop() throws Throwable
   {
      serviceController.shutdown();

      mbeanServer.unregisterMBean(CLASSLOADER_SYSTEM_OBJECT_NAME);
   }

   public void destroy()
   {
      if (createdMBeanServer)
         MBeanServerFactory.releaseMBeanServer(mbeanServer);

      removeProperties();
   }

   public void setProperties(Properties properties)
   {
      this.properties = properties;
   }

   public void setMbeanServer(MBeanServer mbeanServer)
   {
      this.mbeanServer = mbeanServer;
   }

   public ServiceController getServiceController()
   {
      return serviceController;
   }

   public MBeanServer getMbeanServer()
   {
      return mbeanServer;
   }
}
