/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
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
package org.jboss.osgi.framework.bundle;

import org.jboss.dependency.spi.tracker.ContextTracker;
import org.jboss.kernel.Kernel;
import org.jboss.kernel.spi.qualifier.QualifierMatchers;
import org.jboss.metadata.plugins.loader.memory.MemoryMetaDataLoader;
import org.jboss.metadata.spi.MutableMetaData;
import org.jboss.metadata.spi.repository.MutableMetaDataRepository;
import org.jboss.metadata.spi.retrieval.MetaDataRetrieval;
import org.jboss.metadata.spi.retrieval.MetaDataRetrievalFactory;
import org.jboss.metadata.spi.scope.CommonLevels;
import org.jboss.metadata.spi.scope.ScopeKey;

/**
 * Apply MDR usage.
 *
 * @author <a href="ales.justin@jboss.org">Ales Justin</a>
 */
public class MetaDataRetrievalService
{
   /** The kernel */
   private Kernel kernel;
   /** The system tracker */
   private ContextTracker systemTracker;
   /** The mdr factory */
   private MetaDataRetrievalFactory factory;
   /** The previous context tracker */
   private ContextTracker previousTracker;

   public MetaDataRetrievalService(Kernel kernel, ContextTracker systemTracker)
   {
      if (kernel == null)
         throw new IllegalArgumentException("Null kernel");
      if (systemTracker == null)
         throw new IllegalArgumentException("Null system tracker");

      this.kernel = kernel;
      this.systemTracker = systemTracker;
   }

   public void start()
   {
      applyMDRUsage(true);
   }

   public void stop()
   {
      applyMDRUsage(false);
   }

   /**
    * Apply OSGi's MDR usage:
    * - add/remove system bundle as default context tracker
    * - add/remove instance metadata retrieval factory
    *
    * @param register do we register or unregister
    */
   private void applyMDRUsage(boolean register)
   {
      MutableMetaDataRepository repository = kernel.getMetaDataRepository().getMetaDataRepository();
      MetaDataRetrieval retrieval = repository.getMetaDataRetrieval(ScopeKey.DEFAULT_SCOPE);
      if (register && retrieval == null)
      {
         retrieval = new MemoryMetaDataLoader(ScopeKey.DEFAULT_SCOPE);
         repository.addMetaDataRetrieval(retrieval);
      }
      if (retrieval != null && retrieval instanceof MutableMetaData)
      {
         MutableMetaData mmd = (MutableMetaData)retrieval;
         if (register)
         {
            previousTracker = mmd.addMetaData(systemTracker, ContextTracker.class);
         }
         else
         {
            if (previousTracker == null)
            {
               mmd.removeMetaData(ContextTracker.class);
               if (retrieval.isEmpty())
                  repository.removeMetaDataRetrieval(retrieval.getScope());
            }
            else
            {
               mmd.addMetaData(previousTracker, ContextTracker.class);
            }
         }
      }

      // osgi ldap filter parsing and matching
      FilterParserAndMatcher fpm = FilterParserAndMatcher.INSTANCE;
      QualifierMatchers matchers = QualifierMatchers.getInstance();

      if (register)
      {
         matchers.addParser(fpm);
         matchers.addMatcher(fpm);

         MetaDataRetrievalFactory mdrFactory = getMetaDataRetrievalFactory();
         repository.addMetaDataRetrievalFactory(CommonLevels.INSTANCE, mdrFactory);
      }
      else
      {
         repository.removeMetaDataRetrievalFactory(CommonLevels.INSTANCE);

         matchers.removeParser(fpm.getHandledContent());
         matchers.removeMatcher(fpm.getHandledType());
      }
   }

   private MetaDataRetrievalFactory getMetaDataRetrievalFactory()
   {
      MetaDataRetrievalFactory mdrFactory = factory;
      if (mdrFactory == null)
      {
         InstanceMetaDataRetrievalFactory imdrf = new InstanceMetaDataRetrievalFactory(kernel);
         imdrf.addFactory(new OSGiServiceStateDictionaryFactory());
         imdrf.addFactory(new KernelDictionaryFactory(kernel.getConfigurator()));
         // add JMX via configuration, as we don't wanna depend on JMX code
         mdrFactory = imdrf;
      }
      return mdrFactory;
   }

   /**
    * Set mdr factory.
    *
    * @param factory the factory
    */
   public void setFactory(MetaDataRetrievalFactory factory)
   {
      this.factory = factory;
   }
}